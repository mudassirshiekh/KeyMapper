package io.github.sds100.keymapper.Adapters

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.Delegates.ISimpleItemAdapter
import io.github.sds100.keymapper.Interfaces.OnItemClickListener

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display app shortcuts in a RecyclerView
 */
class AppShortcutAdapter(
        override val onItemClickListener: OnItemClickListener<ResolveInfo>,
        private var mAppShortcutList: List<ResolveInfo>,
        private val mPackageManager: PackageManager
) : BaseRecyclerViewAdapter<RecyclerView.ViewHolder>(), ISimpleItemAdapter<ResolveInfo>, Filterable {

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mAppShortcutList,
            onFilter = { filteredList ->
                mAppShortcutList = filteredList
                notifyDataSetChanged()
            },
            getItemText = { getItemText(it) }
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super<ISimpleItemAdapter>.onBindViewHolder(holder, position)
    }

    override fun getFilter() = mAlphabeticalFilter

    override fun getItemCount() = mAppShortcutList.size

    override fun getItem(position: Int) = mAppShortcutList[position]

    override fun getItemText(item: ResolveInfo): String {
        return item.loadLabel(mPackageManager).toString()
    }

    override fun getItemDrawable(item: ResolveInfo): Drawable? {
        return item.loadIcon(mPackageManager)
    }
}