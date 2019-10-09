/*
 * Copyright 2019 ACINQ SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.acinq.eclair.phoenix.main

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import fr.acinq.eclair.CoinUnit
import fr.acinq.eclair.db.*
import fr.acinq.eclair.payment.PaymentRequest
import fr.acinq.eclair.phoenix.NavGraphMainDirections
import fr.acinq.eclair.phoenix.R
import fr.acinq.eclair.phoenix.utils.Converter
import kotlinx.android.synthetic.main.holder_payment.view.*

class PaymentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

  private fun getAttrColor(resId: Int): Int {
    val typedValue = TypedValue()
    itemView.context.theme.resolveAttribute(resId, typedValue, true)
    val ta = itemView.context.obtainStyledAttributes(typedValue.resourceId, intArrayOf(resId))
    val color = ta.getColor(0, 0)
    ta.recycle()
    return color
  }

  @SuppressLint("SetTextI18n")
  fun bindPaymentItem(position: Int, payment: Payment, fiatCode: String, coinUnit: CoinUnit, displayAmountAsFiat: Boolean) {

    val primaryColor: Int = getAttrColor(R.attr.colorPrimary)
    val defaultTextColor: Int = getAttrColor(R.attr.defaultTextColor)
    val mutedTextColor: Int = getAttrColor(R.attr.defaultMutedTextColor)

    val isPaymentOutgoing = payment.direction() is PaymentDirection.`OutgoingPaymentDirection$`
    val amountView = itemView.findViewById<TextView>(R.id.amount)
    val unitView = itemView.findViewById<TextView>(R.id.unit)
    val descriptionView = itemView.findViewById<TextView>(R.id.description)
    val timestampView = itemView.findViewById<TextView>(R.id.timestamp)
    val avatarBgView = itemView.findViewById<ImageView>(R.id.avatar_background)
    val avatarView = itemView.findViewById<ImageView>(R.id.avatar)

    // payment status ====> amount/colors/unit/description/avatar
    when (payment.status()) {
      is OutgoingPaymentStatus.Succeeded, is IncomingPaymentStatus.Received -> {
        // amount
        if (payment.finalAmount().isDefined) {
          if (displayAmountAsFiat) {
            amountView.text = Converter.printFiatPretty(itemView.context, payment.finalAmount().get(), withUnit = false, withSign = true, isOutgoing = isPaymentOutgoing)
          } else {
            amountView.text = Converter.printAmountPretty(payment.finalAmount().get(), itemView.context, withUnit = false, withSign = true, isOutgoing = isPaymentOutgoing)
          }

        } else {
          amountView.text = itemView.context.getString(R.string.utils_unknown)
        }
        // color
        if (isPaymentOutgoing) {
          amountView.amount.setTextColor(ContextCompat.getColor(itemView.context, R.color.dark))
        } else {
          amountView.amount.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
        }
        amountView.visibility = View.VISIBLE
        // unit
        unitView.text = if (displayAmountAsFiat) fiatCode else coinUnit.shortLabel()
        unitView.visibility = View.VISIBLE
        // desc + avatar
        descriptionView.setTextColor(itemView.context.getColor(R.color.dark))
        avatarBgView.imageTintList = ColorStateList.valueOf(primaryColor)
        avatarView.setImageDrawable(itemView.context.getDrawable(R.drawable.payment_holder_def_success))

        // timestamp
        if (payment.completedAt().isDefined) {
          val l: Long = payment.completedAt().get() as Long
          val delaySincePayment: Long = l - System.currentTimeMillis()
          timestampView.text = DateUtils.getRelativeTimeSpanString(l, System.currentTimeMillis(), delaySincePayment)
          timestampView.visibility = View.VISIBLE
        } else {
          timestampView.visibility = View.GONE
        }
      }
      is OutgoingPaymentStatus.`Pending$` -> {
        amountView.visibility = View.GONE
        unitView.visibility = View.GONE
        // desc + avatar
        descriptionView.setTextColor(itemView.context.getColor(R.color.dark))
        avatarBgView.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.transparent))
        avatarView.setImageDrawable(itemView.context.getDrawable(R.drawable.payment_holder_def_pending))

        timestampView.visibility = View.VISIBLE
        timestampView.text = itemView.context.getString(R.string.paymentholder_processing)
      }
      is OutgoingPaymentStatus.Failed -> {
        amountView.visibility = View.GONE
        unitView.visibility = View.GONE
        // desc + avatar
        descriptionView.setTextColor(itemView.context.getColor(R.color.brandy))
        avatarBgView.imageTintList = ColorStateList.valueOf(itemView.context.getColor(R.color.transparent))
        avatarView.setImageDrawable(itemView.context.getDrawable(R.drawable.payment_holder_def_failed))

        // timestamp
        if (payment.completedAt().isDefined) {
          val l: Long = payment.completedAt().get() as Long
          val delaySincePayment: Long = l - System.currentTimeMillis()
          timestampView.text = DateUtils.getRelativeTimeSpanString(l, System.currentTimeMillis(), delaySincePayment)
          timestampView.visibility = View.VISIBLE
        } else {
          timestampView.visibility = View.GONE
        }

      }
    }

    // description
    val desc = if (payment.paymentRequest().isDefined) PaymentRequest.fastReadDescription(payment.paymentRequest().get()) else ""
    if (desc.isNullOrBlank()) {
      descriptionView.text = itemView.context.getString(R.string.paymentholder_no_desc)
      descriptionView.setTextColor(mutedTextColor)
    } else {
      descriptionView.text = desc
      descriptionView.setTextColor(defaultTextColor)
    }

    // clickable action
    itemView.setOnClickListener {
      val action = NavGraphMainDirections.globalActionAnyToPaymentDetails(payment.direction().toString(), if (isPaymentOutgoing && payment.id().isDefined) payment.id().get().toString() else payment.paymentHash().toString(), fromEvent = false)
      it.findNavController().navigate(action)
    }
  }
}
