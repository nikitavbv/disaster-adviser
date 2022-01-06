import { oauthClientId } from '../constants';

export type GoogleOAuthToken = string;

export const getGoogleToken = (): Promise<GoogleOAuthToken> => new Promise((resolve, reject) => {
    const scope = 'https://www.googleapis.com/auth/calendar';
    const redirect_uri = document.location.protocol + '//' + document.location.host + '/oauth';

    const url = `https://accounts.google.com/o/oauth2/v2/auth` +
        `?scope=${scope}` +
        `&response_type=token` +
        `&client_id=${oauthClientId}` +
        `&redirect_uri=${redirect_uri}`;

    const tab = window.open(
        url,
        'Authentication',
        'height=1000,width=1000,modal=yes,alwaysRaised=yes'
    );

    if (tab !== null) {
        const timer = setInterval(() => {
            try {
                const match = /^#access_token=(.*)&token_type/.exec(tab.document.location.hash);
                if (match) {
                    tab.close();
                    clearInterval(timer);
                    resolve(match[1]);
                } else if (tab.document.location.hash === '#error=access_denied') {
                    tab.close();
                    reject('You have to allow access to your Google profile to use this app.');
                }
            } catch(e) {
                console.log('got error while checking auth tag', e);
            }
        }, 100);
    } else {
        console.error('failed to open oauth tab');
    }
});