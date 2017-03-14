const hmac_sha256 = require('crypto-js/hmac-sha256');
const request = require('request');

const functions = require('firebase-functions');

// in order to be able to create custom token we need to initialize Firebase 
// Admin SDK with private key
// https://firebase.google.com/docs/admin/setup
const serviceAccount = require('./service-account-key.json');
const admin = require('firebase-admin');
const firebaseConfig = functions.config().firebase;
firebaseConfig.credential = admin.credential.cert(serviceAccount);
admin.initializeApp(firebaseConfig);

exports.getCustomToken = functions.https.onRequest((req, res) => {
    const accessToken = req.query.access_token || '';
    const facebookAppSecret = functions.config().facebook.app_secret;
    const appSecretProof = hmac_sha256(accessToken, facebookAppSecret);

    // validate Facebook Account Kit access token
    // https://developers.facebook.com/docs/accountkit/graphapi
    request({
        url: `https://graph.accountkit.com/v1.1/me/?access_token=${accessToken}&appsecret_proof=${appSecretProof}`,
        json: true
    }, (error, fbResponse, data) => {
        if (error) {
            console.error('Access token validation request failed\n', error);
            res.status(400).send(error);
        } else if (data.error) {
            console.error('Invalid access token\n', 
                `access_token=${accessToken}\n`, 
                `appsecret_proof=${appSecretProof}\n`, 
                data.error);
            res.status(400).send(data);
        } else {
            admin.auth().createCustomToken(data.id)
                .then(customToken => res.status(200).send(customToken))
                .catch(error => {
                    console.error('Creating custom token failed:', error);
                    res.status(400).send(error);
                })
        }
    });
});
