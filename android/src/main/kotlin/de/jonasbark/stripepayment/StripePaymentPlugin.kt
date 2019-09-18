package de.jonasbark.stripepayment

import androidx.fragment.app.FragmentActivity
import com.stripe.android.model.StripeIntent
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

class StripePaymentPlugin(private val activity: FragmentActivity) : MethodCallHandler {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "stripe_payment")
            channel.setMethodCallHandler(StripePaymentPlugin(registrar.activity() as FragmentActivity))
        }
    }

    var publishableKey: String? = null
    var timeout3DSecure: Int? = null
    var setupIntentClientSecret: String? = null

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "createPaymentMethod" -> {
                publishableKey?.let { key ->
                    StripeDialog.newInstance("Add Source", key, timeout3DSecure, setupIntentClientSecret).apply {

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
}
