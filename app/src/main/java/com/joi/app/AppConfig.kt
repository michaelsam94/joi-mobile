package com.joi.app

object AppConfig {
    /**
     * Where the Joi backend lives. `10.0.2.2` is the special alias the Android EMULATOR uses to
     * reach your machine's own `localhost` — so this default works out of the box against
     * `npm run dev` on the same computer running the emulator. For a physical device or a real
     * deployment, change this to your machine's LAN IP or your deployed URL (must end in `/`).
     */
    const val BASE_URL: String = "http://10.0.2.2:3000/"

    /** Verbose OkHttp request/response logging — leave off for anything you'd call a release build. */
    const val DEBUG_NETWORK_LOGGING: Boolean = true
}
