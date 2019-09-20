#import <Flutter/Flutter.h>
#import "STPAddSourceViewController.h"

@interface StripePaymentPlugin : NSObject<FlutterPlugin, STPAddPaymentMethodViewControllerDelegate>

@property int timeout3DSecure;
@property (nonatomic, nullable) NSString *setupIntentId;

@end
