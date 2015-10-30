package com.keepcloseapp.keepclose;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.regions.Regions;

public class AccountIDProvider extends AWSAbstractCognitoDeveloperIdentityProvider {

    private static final String developerProvider = "login.keepcloseapp.keepclose";

    public AccountIDProvider(String accountId, String identityPoolId, Regions region)
    {
        super(accountId,identityPoolId,region);
    }

    // Return the developer provider name which you choose while setting up the
    // identity pool in the Amazon Cognito Console

    @Override
    public String getProviderName() {
        return developerProvider;
    }

    // Use the refresh method to communicate with your backend to get an
    // identityId and token.

    @Override
    public String refresh() {

        // Override the existing token
        setToken(null);

        // Get the identityId and token by making a call to your backend
        // (Call to your backend)

        // Call the update method with updated identityId and token to make sure
        // these are ready to be used from Credentials Provider.

        update(identityId, token);
        return token;

    }

    // If the app has a valid identityId return it, otherwise get a valid
    // identityId from your backend.

    @Override
    public String getIdentityId() {

        // Load the identityId from the cache
        String cachedIdentityId = "";

        identityId = cachedIdentityId;

        if (identityId == null) {
            // Call to your backend
            return identityId;
        } else {
            return identityId;
        }

    }


}
