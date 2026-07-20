package com.example.pdascanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pdascanner.databinding.ItemAlbaranBinding
import com.example.pdascanner.ui.viewmodel.AlbaranConFotos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlbaranAdapter(
    private var albaranes: List<AlbaranConFotos>
) : RecyclerView.Adapter<AlbaranAdapter.AlbaranViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbaranViewHolder {
        val binding = ItemAlbaranBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlbaranViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbaranViewHolder, position: Int) {
        holder.bind(albaranes[position])
    }

    override fun getItemCount(): Int = albaranes.size

    /**
     * Actualiza la lista de albaranes
     */
    fun actualizarLista(nuevaLista: List<AlbaranConFotos>) {
        albaranes = nuevaLista
        notifyDataSetChanged()
    }

    inner class AlbaranViewHolder(
        private val binding: ItemAlbaranBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AlbaranConFotos) {
            binding.apply {
                // 1. Código de transporte (Siempre visible)
                txtCodigoTransporte.text = item.albaran.codigoTransporte

                // 2. Código de cliente (Lógica para ocultar "DESCONOCIDO" o vacíos)
                val cliente = item.albaran.codigoCliente
                if (cliente.equals("DESCONOCIDO", ignoreCase = true) || cliente.isEmpty()) {
                    txtCodigoCliente.visibility = View.GONE
                } else {
                    txtCodigoCliente.visibility = View.VISIBLE
                    txtCodigoCliente.text = cliente
                }

                // 3. Total de fotos asociadas
                txtTotalFotos.text = "${item.totalFotos} fotos"

                // 4. Fecha de creación
                val fechaFormato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(Date(item.albaran.fecha))
                txtFecha.text = fechaFormato
            }
        }
    }
}