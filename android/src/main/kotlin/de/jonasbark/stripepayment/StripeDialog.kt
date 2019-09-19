package de.jonasbark.stripepayment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.stripe.android.*
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.CardMultilineWidget
import kotlinx.android.synthetic.main.activity_stripe.*

class StripeDialog : androidx.fragment.app.DialogFragment() {

    companion object {
        fun newInstance(
                title: String,
                publishableKey: String,
                timeout3DSecure: Int? = null,
                setupIntentId: String? = null
        ): StripeDialog {
            val frag = StripeDialog()
            val args = Bundle()
            args.putString("title", title)
            args.putString("publishableKey", publishableKey)
            args.putString("setupIntentId", setupIntentId)

            timeout3DSecure?.let {
                args.putInt("timeout3DSecure", it)
            }

            frag.arguments = args
            return frag
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

    private fun context(): Context = context ?: throw RuntimeException("StripeDialog context is null at this point")

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_AppCompat_Light_Dialog)
    }

    var tokenListener: ((StripeIntent.Status, String?) -> (Unit))? = null

    private fun getToken() {
        val mCardInputWidget = card_input_widget as CardMultilineWidget

        if (mCardInputWidget.validateAllFields()) {

            mCardInputWidget.card?.let { card ->

                progress.visibility = View.VISIBLE
                buttonSave.visibility = View.GONE

                val publishableKey = arguments?.getString("publishableKey", null) ?: ""
                PaymentConfiguration.init(context(), publishableKey)

                if(arguments?.containsKey("timeout3DSecure") == true) {
                    configure3DSecure(arguments?.getInt("timeout3DSecure", 5) ?: 5)
                }

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

                            result.id?.let { pmId ->
                                arguments?.getString("setupIntentId", null)?.let {
                                    confirmSetupIntent(stripe, pmId, it)
                                } ?: run {
                                    tokenListener?.invoke(StripeIntent.Status.Succeeded, pmId)
                                    dismiss()
                                }
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
            showUserError()
        }
    }

    private fun showUserError() {
        view?.let {
            Snackbar.make(it, "The card info you entered is not correct", Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    private fun configure3DSecure(timeout: Int) {
        val uiCustomization = PaymentAuthConfig.Stripe3ds2UiCustomization.Builder().build()
        PaymentAuthConfig.init(PaymentAuthConfig.Builder()
                .set3ds2Config(PaymentAuthConfig.Stripe3ds2Config.Builder()
                        // set a 5 minute timeout for challenge flow
                        .setTimeout(timeout)
                        // customize the UI of the challenge flow
                        .setUiCustomization(uiCustomization)
                        .build())
                .build())

    }

    private fun confirmSetupIntent(stripe: Stripe, paymentMethodId: String, setupIntentSecret: String) {
        val params = ConfirmSetupIntentParams.create(paymentMethodId, setupIntentSecret)
        stripe.confirmSetupIntent(this, params)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val stripe = Stripe(activity!!, PaymentConfiguration.getInstance(context()).publishableKey)

        stripe.onSetupResult(50001, data,
                object : ApiResultCallback<SetupIntentResult> {
                    override fun onSuccess(result: SetupIntentResult) {
                        val status = result.intent.status ?: StripeIntent.Status.Canceled
                        if (status == StripeIntent.Status.Succeeded) {
                            val pmId: String = result.intent.paymentMethodId!!
                            tokenListener?.invoke(status, pmId)
                            dismiss()
                        } else {
                            tokenListener?.invoke(status, null)
                            dismiss()
                        }
                    }

                    override fun onError(e: Exception) {
                        Log.e("STRIPE", "Error handling SetupIntent result: ${e.message}", e)
                    }
                })
    }

}
