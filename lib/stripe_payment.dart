import 'dart:async';

import 'package:flutter/services.dart';

class StripeSource {
  static const MethodChannel _channel = const MethodChannel('stripe_payment');

  /// opens the stripe dialog to add a new card
  /// if the source has been successfully added the card token will be returned
  static Future<String> createPaymentMethod() async {
    try {
      final String token = await _channel.invokeMethod('createPaymentMethod');
      return token;
    } on PlatformException catch(e) {
      return Future.error("Stripe create payment method failed: ${e.message}");
    }
  }

  static bool _publishableKeySet = false;

  static bool get ready => _publishableKeySet;

  /// set the publishable key that stripe should use
  /// call this once and before you use [addSource]
  static void setPublishableKey(String apiKey) => init(apiKey);

  static void init(String publishableKey, { int timeout3DSecure, String setupIntentSecret }) {
    if(publishableKey == null) {
      throw "Publishable key is required";
    } else {
      _channel.invokeMethod('setPublishableKey', publishableKey);
      _publishableKeySet = true;
    }

    if(timeout3DSecure != null) {
      if(timeout3DSecure < 5) {
        throw "Invalid 3DSecure timeout. Minimum is 5 (minutes).";
      } else {
        _channel.invokeMethod('timeout3DSecure', timeout3DSecure);
      }
    }

    if(setupIntentSecret != null) {
      _channel.invokeMethod('setSetupIntentClientSecret', setupIntentSecret);
    }
  }

}
