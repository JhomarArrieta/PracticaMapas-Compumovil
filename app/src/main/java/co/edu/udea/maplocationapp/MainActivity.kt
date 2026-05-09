package co.edu.udea.maplocationapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import co.edu.udea.maplocationapp.databinding.ActivityMainBinding
import co.edu.udea.maplocationapp.R


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // ─── Referencias principales ───────────────────────────────────────────
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding

    // ─── Launcher para solicitar permisos en runtime ───────────────────────
    // Este es el enfoque moderno (reemplaza onRequestPermissionsResult)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted  = permissions[Manifest.permission.ACCESS_FINE_LOCATION]  ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            // El usuario concedió el permiso → obtener ubicación
            getCurrentLocation()
        } else {
            Toast.makeText(
                this,
                "Permiso denegado. No se puede obtener la ubicación.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ─── onCreate ──────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar el cliente de Fused Location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicializar el mapa de forma asíncrona
        // Cuando esté listo, se llamará onMapReady()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Botón para obtener ubicación
        binding.btnGetLocation.setOnClickListener {
            checkPermissionsAndGetLocation()
        }
    }

    // ─── onMapReady: el mapa está listo para usarse ────────────────────────
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Habilitar controles de zoom en pantalla
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        // Posición inicial: Medellín, Colombia
        val medellin = LatLng(6.2442, -75.5812)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(medellin, 12f))
    }

    // ─── Flujo de permisos ─────────────────────────────────────────────────
    private fun checkPermissionsAndGetLocation() {
        when {
            // Caso 1: ya tiene permiso → obtener ubicación directamente
            hasLocationPermission() -> {
                getCurrentLocation()
            }
            // Caso 2: ya rechazó antes → mostrar explicación antes de volver a pedir
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionRationale()
            }
            // Caso 3: primera vez → pedir permiso directamente
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de ubicación necesario")
            .setMessage(
                "Esta app necesita acceder a tu ubicación para mostrarte " +
                        "en el mapa. Por favor concede el permiso."
            )
            .setPositiveButton("Conceder") { _, _ -> requestLocationPermission() }
            .setNegativeButton("Cancelar")  { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // ─── Obtener la ubicación actual con Fused Location ───────────────────
    @SuppressLint("MissingPermission") // Ya verificamos el permiso antes de llegar aquí
    private fun getCurrentLocation() {
        // getLastLocation: rápido, devuelve la última posición conocida
        // Puede ser null si el emulador no tiene ubicación configurada
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    updateMapWithLocation(latLng, location.accuracy)
                } else {
                    // Null = emulador sin ubicación configurada
                    Toast.makeText(
                        this,
                        "Ubicación no disponible.\n" +
                                "Configúrala en: Emulador → Extended Controls → Location",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error al obtener ubicación: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ─── Actualizar el mapa con la ubicación obtenida ─────────────────────
    private fun updateMapWithLocation(latLng: LatLng, accuracy: Float) {
        // Limpiar marcadores anteriores
        mMap.clear()

        // Agregar marcador en la posición actual
        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Mi ubicación actual")
                .snippet("Precisión: ${"%.1f".format(accuracy)} metros")
        )

        // Animar la cámara hacia la posición con zoom 16
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        )

        // Mostrar coordenadas en la card
        binding.tvLatitude.text  = "Latitud:   ${"%.6f".format(latLng.latitude)}"
        binding.tvLongitude.text = "Longitud: ${"%.6f".format(latLng.longitude)}"
        binding.tvAccuracy.text  = "Precisión: ${"%.1f".format(accuracy)} m"
        binding.cardCoordinates.visibility = View.VISIBLE
    }
}
