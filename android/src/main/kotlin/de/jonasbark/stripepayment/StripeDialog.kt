package de.jonasbark.stripepayment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.view.CardMultilineWidget
import kotlinx.android.synthetic.main.activity_stripe.*

class StripeDialog : androidx.fragment.app.DialogFragment() {

    companion object {
        fun newInstance(title: String, publishableKey: String): StripeDialog {
            val frag = StripeDialog()
            val args = Bundle()
            args.putString("title", title)
            args.putString("publishableKey", publishableKey)
            frag.arguments = args
            return frag
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_stripe, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get field from view
        // Fetch arguments from bundle and set title
        val title = arguments?.getString("title", "Add Source")
        dialog?.setTitle(title)

        buttonSave.setOnClickListener {
            getToken()
        }

    }

    fun context(): Context = context ?: throw RuntimeException("StripeDialog context is null at this point")

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_AppCompat_Light_Dialog)
    }

    var tokenListener: ((String) -> (Unit))? = null

    private fun getToken() {
        val mCardInputWidget = card_input_widget as CardMultilineWidget

        if (mCardInputWidget.validateAllFields()) {

            mCardInputWidget.card?.let { card ->

                progress.visibility = View.VISIBLE
                buttonSave.visibility = View.GONE

                val publishableKey = arguments?.getString("publishableKey", null) ?: ""
                PaymentConfiguration.init(context(), publishableKey)

                val paymentMethodParamsCard = card.toPaymentMethodParamsCard()
                val paymentMethodCreateParams = PaymentMethodCreateParams.create(
                    paymentMethodParamsCard,
                    PaymentMethod.BillingDetails.Builder().build()
                )

                val stripe = Stripe(activity!!, PaymentConfiguration.getInstance(context()).publishableKey)

                stripe.createPaymentMethod(
                    paymentMethodCreateParams,
                    object : ApiResultCallback<PaymentMethod> {
                        override fun onSuccess(result: PaymentMethod) {
                            progress.visibility = View.GONE
                            buttonSave.visibility = View.GONE

                            if (result.id != null) {
                                tokenListener?.invoke(result.id!!)
                                dismiss()
                            }
                        }

                        override fun onError(error: Exception) {
                            progress.visibility = View.GONE
                            buttonSave.visibility = View.VISIBLE
                            view?.let {
                                Snackbar.make(it, error.localizedMessage, Snackbar.LENGTH_LONG)
                                    .show()
                            }
                        }

                    })

            }
        } else {
            view?.let {
                Snackbar.make(it, "The card info you entered is not correct", Snackbar.LENGTH_LONG)
                    .show()
            }
        }

    }
}
