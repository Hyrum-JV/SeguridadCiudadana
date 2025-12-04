const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Radio de búsqueda de 1 km (en grados de latitud/longitud para una aproximación)
const RADIO_BUSQUEDA_KM = 1.0; 
const GRADOS_POR_KM = 0.009; 
const GRADOS_RADIO = RADIO_BUSQUEDA_KM * GRADOS_POR_KM; 

exports.notificarReporteCercano = functions.firestore
    .document('reportes/{reporteId}')
    .onCreate(async (snap, context) => {

        const nuevoReporte = snap.data();
        const reporteGeoPoint = nuevoReporte.ubicacion;
        const remitenteId = nuevoReporte.userId;
        const categoria = nuevoReporte.categoria;
            
        if (!reporteGeoPoint) {
            console.log("Reporte sin GeoPoint, saltando notificación.");
            return null;
        }

        const reportLat = reporteGeoPoint.latitude;
        const reportLon = reporteGeoPoint.longitude;

        const tokensCercanos = [];

        // 1. Obtener a todos los usuarios para revisar su token y ubicación
        const usuariosSnapshot = await admin.firestore().collection('usuarios').get();

        usuariosSnapshot.forEach(doc => {
            const userData = doc.data();
            const usuarioId = doc.id;
            const userToken = userData.fcmToken;
            const userGeoPoint = userData.ubicacion; 

            // 2. Filtro de Exclusión: No notificar al remitente
            if (usuarioId === remitenteId) {
                return; 
            }

            // 3. Filtro de Cercanía
            if (userGeoPoint && userToken) {
                // Cálculo de la distancia aproximada
                const distanciaAproximadaLat = Math.abs(userGeoPoint.latitude - reportLat);
                const distanciaAproximadaLon = Math.abs(userGeoPoint.longitude - reportLon);

                if (distanciaAproximadaLat < GRADOS_RADIO && distanciaAproximadaLon < GRADOS_RADIO) {
                    tokensCercanos.push(userToken);
                }
            }
        });

        if (tokensCercanos.length === 0) {
            console.log("No se encontraron usuarios cercanos para notificar.");
            return null;
        }

        // 4. Crear el mensaje FCM
        const payload = {
            notification: {
                title: '🚨 ¡NUEVO REPORTE DE SEGURIDAD!',
                body: `Se ha reportado "${categoria}" cerca de tu zona. Revisa el mapa.`,
                sound: 'default',
            },
            data: {
                reporteId: snap.id,
                tipo: 'reporte_cercano'
            }
        };

        // 5. Enviar las notificaciones a los tokens cercanos
        const response = await admin.messaging().sendToDevice(tokensCercanos, payload);
        console.log('Notificaciones enviadas con éxito:', response.successCount, 'fallidas:', response.failureCount);

        return null;
    });

/**
 * Notificar a los administradores cuando se crea un nuevo reporte
 * Envía notificación al topic 'admin_reportes' al que están suscritos los admins
 */
exports.notificarAdminsNuevoReporte = functions.firestore
    .document('reportes/{reporteId}')
    .onCreate(async (snap, context) => {
        const nuevoReporte = snap.data();
        const categoria = nuevoReporte.categoria || 'Reporte';
        const descripcion = nuevoReporte.descripcion || 'Sin descripción';
        const reporteId = context.params.reporteId;

        // Crear mensaje para el topic de administradores
        const message = {
            topic: 'admin_reportes',
            notification: {
                title: `🚨 Nuevo Reporte: ${categoria}`,
                body: descripcion.length > 100 ? descripcion.substring(0, 100) + '...' : descripcion,
            },
            data: {
                reporteId: reporteId,
                tipo: 'nuevo_reporte_admin',
                categoria: categoria,
                timestamp: Date.now().toString()
            },
            android: {
                priority: 'high',
                notification: {
                    channelId: 'admin_new_reports',
                    priority: 'high',
                    defaultVibrateTimings: true,
                    defaultSound: true
                }
            }
        };

        try {
            const response = await admin.messaging().send(message);
            console.log('Notificación enviada a admins:', response);
            return response;
        } catch (error) {
            console.error('Error enviando notificación a admins:', error);
            return null;
        }
    });

/**
 * Notificar al ciudadano cuando su reporte cambia de estado
 */
exports.notificarCambioEstadoReporte = functions.firestore
    .document('reportes/{reporteId}')
    .onUpdate(async (change, context) => {
        const antes = change.before.data();
        const despues = change.after.data();
        const reporteId = context.params.reporteId;

        // Solo notificar si el estado cambió
        if (antes.estado === despues.estado) {
            return null;
        }

        const userId = despues.userId;
        if (!userId) {
            console.log("Reporte sin userId, no se puede notificar");
            return null;
        }

        // Obtener el token FCM del usuario
        const userDoc = await admin.firestore().collection('usuarios').doc(userId).get();
        if (!userDoc.exists) {
            console.log("Usuario no encontrado");
            return null;
        }

        const userData = userDoc.data();
        const fcmToken = userData.fcmToken;

        if (!fcmToken) {
            console.log("Usuario sin token FCM");
            return null;
        }

        // Crear mensaje de notificación según el nuevo estado
        let titulo = '';
        let mensaje = '';

        switch (despues.estado) {
            case 'Policía verificando':
                titulo = '👮 Policía en Camino';
                mensaje = `Tu reporte "${despues.categoria}" está siendo verificado por la policía.`;
                break;
            case 'Pendiente de resolución':
                titulo = '⏳ Reporte en Proceso';
                mensaje = `Tu reporte "${despues.categoria}" está pendiente de resolución.`;
                break;
            case 'Caso resuelto':
                titulo = '✅ Caso Resuelto';
                mensaje = `Tu reporte "${despues.categoria}" ha sido resuelto. ¡Gracias por colaborar!`;
                break;
            case 'Noticia falsa':
                titulo = '❌ Reporte Descartado';
                mensaje = `Tu reporte "${despues.categoria}" fue marcado como noticia falsa.`;
                break;
            default:
                titulo = '📋 Actualización de Reporte';
                mensaje = `Tu reporte "${despues.categoria}" cambió a: ${despues.estado}`;
        }

        const payload = {
            token: fcmToken,
            notification: {
                title: titulo,
                body: mensaje,
            },
            data: {
                reporteId: reporteId,
                tipo: 'cambio_estado',
                nuevoEstado: despues.estado
            },
            android: {
                priority: 'high',
                notification: {
                    channelId: 'report_updates',
                    priority: 'high'
                }
            }
        };

        try {
            const response = await admin.messaging().send(payload);
            console.log('Notificación de cambio de estado enviada:', response);
            return response;
        } catch (error) {
            console.error('Error enviando notificación de cambio de estado:', error);
            return null;
        }
    });