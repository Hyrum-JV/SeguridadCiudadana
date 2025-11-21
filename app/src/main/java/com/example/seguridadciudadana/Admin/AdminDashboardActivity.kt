package com.example.seguridadciudadana.Admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.seguridadciudadana.Inicio.ReporteZona
import com.example.seguridadciudadana.R
import com.example.seguridadciudadana.Registro.RolUsuarioActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var rvReports: RecyclerView
    private lateinit var rvStats: RecyclerView
    private lateinit var spinnerFilter: Spinner
    private lateinit var etSearch: EditText
    private lateinit var btnRefresh: Button
    private lateinit var toolbar: Toolbar
    private lateinit var adapter: AdminReportAdapter
    private lateinit var statAdapter: StatAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val db = FirebaseFirestore.getInstance()
    private var allReports = listOf<ReporteZona>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configurar Google Sign-In para cerrar sesión
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configurar Toolbar
        toolbar = findViewById(R.id.toolbar_admin)
        setSupportActionBar(toolbar)

        // Inicializar vistas
        rvReports = findViewById(R.id.rv_admin_reports)
        rvStats = findViewById(R.id.rv_stats)
        spinnerFilter = findViewById(R.id.spinner_filter_status)
        etSearch = findViewById(R.id.et_search)
        btnRefresh = findViewById(R.id.btn_refresh)

        // Configurar RecyclerView de reportes
        adapter = AdminReportAdapter { report ->
            val fragment = AdminReportDetailFragment.newInstance(report.id)
            supportFragmentManager.beginTransaction()
                .replace(R.id.admin_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        rvReports.adapter = adapter
        rvReports.layoutManager = LinearLayoutManager(this)

        // Configurar RecyclerView de stats
        statAdapter = StatAdapter()
        rvStats.adapter = statAdapter
        rvStats.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Configurar filtro
        val statuses = arrayOf("Todos", "pending", "police_in_progress", "pending_resolution", "case_resolved", "false_news")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = spinnerAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = statuses[position]
                filterReports(selectedStatus)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Configurar búsqueda
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterReports(spinnerFilter.selectedItem.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        // Configurar botón refrescar
        btnRefresh.setOnClickListener {
            loadReports()
        }

        loadReports()
    }

    // Inflar el menú en el Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    // Manejar clic en opciones del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Mostrar diálogo de confirmación para cerrar sesión
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Cerrar sesión y regresar a selección de rol
    private fun logout() {
        // Cerrar sesión de Firebase
        auth.signOut()

        // Cerrar sesión de Google
        googleSignInClient.signOut().addOnCompleteListener {
            // Redirigir a la pantalla de selección de rol
            val intent = Intent(this, RolUsuarioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadReports() {
        db.collection("reportes").get().addOnSuccessListener { result ->
            allReports = result.documents.map { doc ->
                ReporteZona(
                    id = doc.id,
                    categoria = doc.getString("categoria") ?: "",
                    ubicacion = doc.getGeoPoint("ubicacion"),
                    descripcion = doc.getString("descripcion"),
                    evidenciaUrl = doc.getString("evidenciaUrl"),
                    timestamp = doc.getTimestamp("timestamp"),
                    userId = doc.getString("userId") ?: "",
                    estado = doc.getString("estado") ?: "pending",
                    adminComentario = doc.getString("adminComentario") ?: "",
                    adminUid = doc.getString("adminUid") ?: ""
                )
            }
            filterReports("Todos")
            updateStats()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al cargar reportes: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AdminDashboard", "Error loading reports", e)
        }
    }

    private fun filterReports(status: String) {
        val searchQuery = etSearch.text.toString().lowercase()
        val filtered = allReports.filter { report ->
            val matchesStatus = status == "Todos" || report.estado == status
            val matchesSearch = searchQuery.isEmpty() ||
                    report.categoria.lowercase().contains(searchQuery) ||
                    (report.descripcion?.lowercase()?.contains(searchQuery) == true)
            matchesStatus && matchesSearch
        }
        adapter.updateReports(filtered)
    }

    private fun updateStats() {
        val pendingCount = allReports.count { it.estado == "pending" }
        val inProgressCount = allReports.count { it.estado == "police_in_progress" }
        val resolvedCount = allReports.count { it.estado == "case_resolved" }

        val stats = listOf(
            StatItem("Pendientes", pendingCount, R.drawable.ic_pending),
            StatItem("En Progreso", inProgressCount, R.drawable.ic_in_progress),
            StatItem("Resueltos", resolvedCount, R.drawable.ic_resolved)
        )
        statAdapter.updateStats(stats)
    }
}