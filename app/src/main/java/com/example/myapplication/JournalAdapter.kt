package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import java.io.File

class JournalAdapter(
    private var entries: MutableList<JournalEntry>,
    private val onClick: (JournalEntry) -> Unit
) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    class JournalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_journal_title)
        val tvContent: TextView = view.findViewById(R.id.tv_journal_content)
        val cvImage: CardView = view.findViewById(R.id.cv_journal_image)
        val ivImage: ImageView = view.findViewById(R.id.iv_journal_image)

        // NOWE POLA
        val tvTime: TextView = view.findViewById(R.id.tv_journal_time)
        val tvDateOnly: TextView = view.findViewById(R.id.tv_journal_date_only)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_journal_entry, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val entry = entries[position]

        // 1. Kolor godziny i ikonki z motywu użytkownika
        holder.tvTime.setTextColor(GameManager.appThemeColor)
        holder.itemView.findViewById<ImageView>(R.id.iv_journal_icon).imageTintList = android.content.res.ColorStateList.valueOf(GameManager.appThemeColor)

        holder.tvTitle.text = entry.title
        holder.tvContent.text = entry.content

        // 2. Przypisanie daty i godziny do osobnych pól
        if (entry.time.isNotEmpty()) {
            holder.tvTime.text = entry.time
            holder.tvTime.visibility = View.VISIBLE
        } else {
            holder.tvTime.visibility = View.GONE
        }

        holder.tvDateOnly.text = entry.date

        holder.itemView.setOnClickListener { onClick(entry) }

        // 3. Ładowanie zdjęć nowoczesną biblioteką COIL (Zamiast 100 linijek starego kodu!)
        if (!entry.imageUri.isNullOrEmpty()) {
            val file = File(entry.imageUri)
            if (file.exists()) {
                holder.cvImage.visibility = View.VISIBLE

                // Magia Coila - to jedna linijka załatwia obracanie, cache i wyświetlanie:
                holder.ivImage.load(file) {
                    crossfade(true)
                }
            } else {
                holder.cvImage.visibility = View.GONE
            }
        } else {
            holder.cvImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = entries.size

    fun updateList(newEntries: List<JournalEntry>) {
        entries = newEntries.toMutableList()
        notifyDataSetChanged()
    }
}