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