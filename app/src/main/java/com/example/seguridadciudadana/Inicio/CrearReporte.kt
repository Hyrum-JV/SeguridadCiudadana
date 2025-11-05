package com.example.seguridadciudadana.Inicio

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.example.seguridadciudadana.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.io.File


class CrearReporte : Fragment() {

    private lateinit var spinnerCategoria: Spinner
    private lateinit var layoutCategoriaOtro: TextInputLayout
    private lateinit var etCategoriaOtro: TextInputEditText
    private lateinit var btnAdjuntar : Button
    private lateinit var imgPreview: ImageView
    private lateinit var btnEnviar : Button
    private lateinit var etDescripcion : TextInputEditText


    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvUbicacion: TextView

    private var archivoAdjuntoUri: Uri? = null
    private var ubicacionActual: Location? = null


    private val REQUEST_TOMAR_FOTO = 101
    private val REQUEST_GRABAR_VIDEO = 102
    private val REQUEST_GALERIA = 103


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerCategoria = view.findViewById(R.id.spinnerCategoria)
        layoutCategoriaOtro = view.findViewById(R.id.layoutCategoriaOtro)
        etDescripcion = view.findViewById(R.id.etDescripcion)
        etCategoriaOtro = view.findViewById(R.id.etCategoriaOtro)
        tvUbicacion = view.findViewById(R.id.tvUbicacion)
        btnAdjuntar = view.findViewById(R.id.btnAdjuntar)
        imgPreview = view.findViewById(R.id.imgPreview)
        btnEnviar = view.findViewById(R.id.btnEnviarReporte)


        val categorias = listOf(
            "Robo a mano armada",
            "Asalto a vivienda o local",
            "Balacera",
            "Violencia doméstica",
            "Otro"
        )

        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        obtenerUbicacion()

        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val seleccion = categorias[position]
                layoutCategoriaOtro.visibility = if (seleccion == "Otro") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                layoutCategoriaOtro.visibility = View.GONE
            }
        }

        btnAdjuntar.setOnClickListener {
            val opciones = arrayOf("Tomar foto", "Grabar video", "Elegir desde galería")

            AlertDialog.Builder(requireContext())
                .setTitle("Selecciona una opción")
                .setItems(opciones) { _, which ->
                    when (which) {
                        0 -> abrirCamaraFoto()
                        1 -> abrirCamaraVideo()
                        2 -> abrirGaleria()
                    }
                }
                .show()
        }

        btnEnviar.setOnClickListener {
            val categoriaSeleccionada = spinnerCategoria.selectedItem?.toString()
            val categoriaOtro = etCategoriaOtro.text?.toString()?.trim()
            val descripcion = etDescripcion.text?.toString()?.trim()
            val descripcionFinal = if (descripcion.isNullOrEmpty()) null else descripcion

            // Validación
            if (categoriaSeleccionada.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Selecciona una categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (categoriaSeleccionada == "Otro" && categoriaOtro.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Especifica la categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ubicacionActual == null) {
                Toast.makeText(requireContext(), "Ubicación no disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (archivoAdjuntoUri == null) {
                Toast.makeText(requireContext(), "Adjunta una evidencia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoriaFinal = if (categoriaSeleccionada == "Otro") categoriaOtro!! else categoriaSeleccionada

            subirArchivoYGuardarReporte(categoriaFinal, descripcionFinal)
        }
    }

    private fun subirArchivoYGuardarReporte(categoria: String, descripcion: String?) {
        if (archivoAdjuntoUri == null || ubicacionActual == null) {
            Toast.makeText(requireContext(), "Faltan datos para enviar el reporte", Toast.LENGTH_SHORT).show()
            return
        }

        val reporte = Reporte(
            categoria = categoria,
            descripcion = descripcion,
            ubicacion = GeoPoint(ubicacionActual!!.latitude, ubicacionActual!!.longitude),
            evidenciaLocalUri = archivoAdjuntoUri.toString(),
            timestamp = Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("reportes")
            .add(reporte)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Reporte enviado correctamente", Toast.LENGTH_SHORT).show()
                // Opcional: volver al mapa o limpiar el formulario
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al guardar el reporte", Toast.LENGTH_SHORT).show()
            }
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1002
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                ubicacionActual = location
                val lat = location.latitude
                val lon = location.longitude
                tvUbicacion.text = "Ubicación actual: $lat, $lon"
            } else {
                tvUbicacion.text = "No se pudo obtener la ubicación"
            }
        }
    }

    private fun abrirCamaraFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_TOMAR_FOTO)
        }
    }

    private fun abrirCamaraVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_GRABAR_VIDEO)
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/* video/*"
        startActivityForResult(intent, REQUEST_GALERIA)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_GRABAR_VIDEO -> {
                    val videoUri = data.data
                    if (videoUri != null) {
                        archivoAdjuntoUri = videoUri
                        imgPreview.setImageURI(videoUri)
                    }
                }
                REQUEST_GALERIA -> {
                    val uri = data.data
                    if (uri != null) {
                        archivoAdjuntoUri = uri
                        imgPreview.setImageURI(uri)
                    }
                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crear_reporte, container, false)
    }
}