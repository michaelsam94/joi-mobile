package com.joi.app

object AppConfig {
    /**
     * Where the Joi backend lives. Pointed at the deployed VPS backend, reverse-proxied through
     * Caddy with a real Let's Encrypt certificate (must end in `/`).
     *
     * For local development against `npm run dev` instead, use `http://10.0.2.2:3000/` on the
     * Android EMULATOR (the special alias for your machine's own `localhost`), or your machine's
     * LAN IP for a physical device on the same network.
     */
    const val BASE_URL: String = "https://joi.michaelsam94.com/"

    /** Verbose OkHttp request/response logging — leave off for anything you'd call a release build. */
    const val DEBUG_NETWORK_LOGGING: Boolean = true
}
