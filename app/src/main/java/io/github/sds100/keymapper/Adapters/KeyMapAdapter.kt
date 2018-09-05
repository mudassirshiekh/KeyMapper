package io.github.sds100.keymapper.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Selection.SelectableBackground
import io.github.sds100.keymapper.Utils.ActionUtils
import kotlinx.android.synthetic.main.keymap_adapter_item.view.*

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * Display a list of [KeyMap]s in a RecyclerView
 */
class KeyMapAdapter(keymapList: List<KeyMap>
) : SelectableAdapter<KeyMap, KeyMapAdapter.ViewHolder>(keymapList) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.keymap_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val keyMap = itemList[position]

        holder.itemView.apply {
            val triggerAdapter = TriggerAdapter(keyMap.triggerList, showRemoveButton = false)

            textViewTitle.text = ActionUtils.getDescription(context, keyMap.action)

            recyclerViewTriggers.layoutManager = LinearLayoutManager(context)
            recyclerViewTriggers.adapter = triggerAdapter

            /*if no icon should be shown then hide the ImageView so there isn't whitespace next to
            the text*/
            val drawable = ActionUtils.getIcon(context, keyMap.action)
            if (drawable == null) {
                imageView.setImageDrawable(null)
                imageView.visibility = View.GONE
            } else {
                imageView.setImageDrawable(drawable)
                imageView.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return itemList[position].id
    }

    override fun onStartMultiSelect() {}

    inner class ViewHolder(itemView: View)
        : SelectableAdapter<KeyMap, ViewHolder>.ViewHolder(itemView) {
        init {
            //set the background of the item so it shows an animation when long pressed
            itemView.background = SelectableBackground(itemView.context)
        }
    }
}