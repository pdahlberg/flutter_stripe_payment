package de.jonasbark.stripepayment

import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SetupIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.StripeIntent
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

class StripePaymentPlugin(private val activity: FragmentActivity) : MethodCallHandler, PluginRegistry.ActivityResultListener {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val plugin = StripePaymentPlugin(registrar.activity() as FragmentActivity)

            registrar.addActivityResultListener(plugin)

            val channel = MethodChannel(registrar.messenger(), "stripe_payment")
            channel.setMethodCallHandler(plugin)
        }
    }

    var dialog: StripeDialog? = null
    var publishableKey: String? = null
    var timeout3DSecure: Int? = null
    var setupIntentClientSecret: String? = null

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "createPaymentMethod" -> {
                publishableKey?.let { key ->
                    dialog = StripeDialog.newInstance("Create Payment Method", key, timeout3DSecure, setupIntentClientSecret).apply {

                        show(this@StripePaymentPlugin.activity.supportFragmentManager, "stripe")

                        tokenListener = { status, token ->
                            if(status == StripeIntent.Status.Succeeded) {
                                result.success(token)
                            } else {
                                result.error(status.name, null, null)
                            }
                        }
                    }
                }
                if (publishableKey == null) {
                    result.error("You have to set a publishable key first", null, null)
                }
            }
            "setPublishableKey" -> {
                publishableKey = call.arguments as String
                result.success(null)
            }
            "timeout3DSecure" -> {
                timeout3DSecure = call.arguments as Int
                result.success(null)
            }
            "setSetupIntentClientSecret" -> {
                setupIntentClientSecret = call.arguments as String
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) : Boolean {
        dialog?.onActivityResult(requestCode, resultCode, data)
        return true
    }
}
