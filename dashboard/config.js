var config =
{
    // hostname/port/name/password for Kapua broker
    BROKER_WEBSOCKET_HOSTNAME: process.env.BROKER_WS_NAME + '-' + process.env.OPENSHIFT_BUILD_NAMESPACE,
    BROKER_WEBSOCKET_PORT: process.env.BROKER_WEBSOCKET_PORT || 80,
    BROKER_USERNAME: process.env.BROKER_USERNAME || "demo_username",
    BROKER_PASSWORD: process.env.BROKER_PASSWORD || "demo_password",

    // hostname/port for DG proxy (no username/password required for demo)
    DATASTORE_REST_HOSTNAME: process.env.DATASTORE_PROXY_SERVICE + '-' + process.env.OPENSHIFT_BUILD_NAMESPACE,
    DATASTORE_REST_PORT: process.env.DATASTORE_REST_PORT || 80,

    // Google API Key (can be blank, resulting in throttling for high usage)
    GOOGLE_MAPS_API_KEY: process.env.GOOGLE_MAPS_API_KEY || ""
};

module.exports = config;
