//
//  STPAddSourceViewController.m
//  Runner
//
//  Created by Jonas Bark on 06.11.17.
//  Copyright © 2017 The Chromium Authors. All rights reserved.
//

#import "STPAddSourceViewController.h"

@interface STPAddSourceViewController() <STPAuthenticationContext>

@end

@implementation STPAddSourceViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [self.navigationItem.leftBarButtonItem setTarget:self];
    [self.navigationItem.leftBarButtonItem setAction:@selector(cancelClicked:)];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

-(void)cancelClicked:(id)sender {
    [self.srcDelegate addCardViewControllerDidCancel:self];
}

-(void)nextPressed:(id)sender {
    
    //TODO hackedihack
    STPPaymentCardTextField* paymentCell = [((UITableView*)self.view.subviews.firstObject) cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]].subviews.firstObject.subviews.firstObject;
    
    STPAPIClient *apiClient = [[STPAPIClient alloc] initWithConfiguration:[STPPaymentConfiguration sharedConfiguration]];

    // todo: 3DSecure config
    
    STPPaymentMethodCardParams *cardParams = paymentCell.cardParams;
    STPPaymentMethodParams *paymentMethodParams = [STPPaymentMethodParams paramsWithCard:cardParams billingDetails:nil metadata:nil];

    if (cardParams) {
        [apiClient createPaymentMethodWithParams:paymentMethodParams completion:^(STPPaymentMethod * _Nullable paymentMethod, NSError * _Nullable error) {
            if (error) {
                [self performSelector:@selector(handleError:) withObject:error afterDelay:0];
            } else if(self.setupIntentId != NULL) {
                [self confirmSetupIntent:self.setupIntentId paymentMethod:paymentMethod];
            } else {
                [self returnResult:paymentMethod];
            }
        }];
    }
}

-(void) returnResult:(STPPaymentMethod *)pm {
    NSLog(@"Return result: %@", pm.stripeId);
    [self.srcDelegate addCardViewController:self didCreatePaymentMethod:pm completion:^(NSError * _Nullable error) {
    }];
}

-(void) confirmSetupIntent:(NSString *)siId paymentMethod:(STPPaymentMethod *)pm {
    STPSetupIntentConfirmParams *params = [[STPSetupIntentConfirmParams alloc] initWithClientSecret:siId];
    params.paymentMethodID = pm.stripeId;
    
    NSLog(@"Confirm setup intent: %@", siId);

    [[STPPaymentHandler sharedHandler]
     confirmSetupIntent:params
     withAuthenticationContext:self
     completion:^(STPPaymentHandlerActionStatus status, STPSetupIntent * setupIntent, NSError * error) {
           switch (status) {
               case STPPaymentHandlerActionStatusSucceeded:
                    [self returnResult:pm];
                    break;
                   // Setup succeeded
               case STPPaymentHandlerActionStatusCanceled:
                   break;
                   // Handle cancel
               case STPPaymentHandlerActionStatusFailed:
                   break;
                   // Handle error
           }
       }];
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (nonnull UIViewController *)authenticationPresentingViewController {
    return self;
}

@end
