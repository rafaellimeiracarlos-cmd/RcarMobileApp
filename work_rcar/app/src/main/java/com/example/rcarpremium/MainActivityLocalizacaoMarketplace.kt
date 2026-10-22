package com.example.rcarpremium

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import android.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.Locale

/** Localização aproximada e opcional. Não armazena nem compartilha posição do cliente. */
class MainActivityLocalizacaoMarketplace : RcarBaseActivity() {
    private lateinit var resultados: LinearLayout
    private lateinit var locationManager: LocationManager
    private var listener: LocationListener? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) obterLocalizacao() else mostrarAviso("Você pode continuar comprando sem compartilhar sua localização.")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val (frame, body) = MarketplaceUi.page(this, "LOJAS PERTO DE VOCÊ",
            "Use localização aproximada apenas enquanto esta tela estiver aberta.",
            "⌂  INÍCIO DA LOJA", { finish() }, {
                startActivity(Intent(this, MainActivitycomprasonline::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            })
        body.addView(MarketplaceUi.button(this, "⌖ LOCALIZAR LOJAS PARCEIRAS", true).apply {
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(this@MainActivityLocalizacaoMarketplace, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) obterLocalizacao()
                else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }, MarketplaceUi.lp(this, 15))
        body.addView(MarketplaceUi.text(this, "A posição não é gravada no aparelho nem enviada ao servidor. Distâncias estimadas em linha reta, não são rotas nem previsão de frete.", 12f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 8))
        resultados = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(resultados, MarketplaceUi.lp(this, 17))
        setContentView(frame)
        mostrarAviso("Toque em LOCALIZAR para ordenar lojas reais cadastradas pelo CEO.")
    }
    private fun mostrarAviso(message: String) {
        resultados.removeAllViews(); resultados.addView(MarketplaceUi.card(this).apply {
            addView(MarketplaceUi.text(this@MainActivityLocalizacaoMarketplace, message, 14f, Color.WHITE))
        })
    }
    @Suppress("MissingPermission")
    private fun obterLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter {
            try { locationManager.isProviderEnabled(it) } catch (_: Exception) { false }
        }
        if (providers.isEmpty()) { mostrarAviso("Ative a localização do telefone e tente novamente."); return }
        val last = providers.mapNotNull { provider -> try { locationManager.getLastKnownLocation(provider) } catch (_: Exception) { null } }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 10 * 60 * 1000) { mostrarLojas(last); return }
        mostrarAviso("Buscando localização aproximada... Se não aparecer, verifique se o serviço de localização está ativo.")
        val locListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                mostrarLojas(location); pararBusca()
            }
            override fun onProviderDisabled(provider: String) { mostrarAviso("Localização desativada."); pararBusca() }
        }
        listener = locListener
        try { locationManager.requestLocationUpdates(providers.first(), 0L, 0f, locListener) }
        catch (_: SecurityException) { mostrarAviso("Permissão de localização indisponível.") }
        catch (_: Exception) { mostrarAviso("Não foi possível obter a localização.") }
    }
    private fun mostrarLojas(location: Location) {
        resultados.removeAllViews()
        val lojas = MarketplaceStore.lojas(this).filter { it.ativa && it.latitude != null && it.longitude != null }
            .sortedBy { MarketplaceStore.distanciaKm(location.latitude, location.longitude, it) ?: Double.MAX_VALUE }
        if (lojas.isEmpty()) { mostrarAviso("Nenhuma loja tem coordenadas cadastradas. O CEO pode adicioná-las em Gerenciar lojas."); return }
        resultados.addView(MarketplaceUi.text(this, "${lojas.size} lojas com localização cadastrada", 16f, MarketplaceUi.ORANGE, true))
        lojas.forEach { loja ->
            val card = MarketplaceUi.card(this)
            card.addView(MarketplaceUi.text(this, loja.nome, 17f, Color.WHITE, true))
            val distance = MarketplaceStore.distanciaKm(location.latitude, location.longitude, loja) ?: 0.0
            card.addView(MarketplaceUi.text(this, "${loja.cidade} / ${loja.uf}  •  ~${String.format(Locale("pt", "BR"), "%.1f", distance)} km em linha reta", 13f, MarketplaceUi.MUTED), MarketplaceUi.lp(this, 5))
            card.addView(MarketplaceUi.button(this, "VER NO MAPA", true).apply {
                setOnClickListener {
                    val destination = "${loja.latitude},${loja.longitude}"
                    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(destination)}")
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    catch (_: Exception) { Toast.makeText(this@MainActivityLocalizacaoMarketplace, "Instale um navegador ou aplicativo de mapas.", Toast.LENGTH_LONG).show() }
                }
            }, MarketplaceUi.lp(this, 7))
            resultados.addView(card, MarketplaceUi.lp(this, 9))
        }
    }
    private fun pararBusca() {
        if (::locationManager.isInitialized) listener?.let { try { locationManager.removeUpdates(it) } catch (_: Exception) {} }
        listener = null
    }
    override fun onStop() { pararBusca(); super.onStop() }
}
