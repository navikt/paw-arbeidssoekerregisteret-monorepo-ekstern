package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

class RetryInterceptor(private val maxRetries: Int) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0

        while (!response.isSuccessful && tryCount < maxRetries) {
            tryCount++
            response.close()
            request = request.newBuilder().build()
            response = chain.proceed(request)
        }

        return response
    }
}
