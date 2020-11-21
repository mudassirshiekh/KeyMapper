package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 08/11/20.
 */

data class FingerprintGestureMapListItemModel(
    val id: String,
    val header: String,
    val actionModels: List<ActionModel>,
    val constraintModels: List<ConstraintModel>,
    val isEnabled: Boolean
)