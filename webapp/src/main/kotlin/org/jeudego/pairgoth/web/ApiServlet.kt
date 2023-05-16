package org.jeudego.pairgoth.web

import com.republicate.kson.Json
import org.jeudego.pairgoth.api.ApiHandler
import org.jeudego.pairgoth.api.PairingHandler
import org.jeudego.pairgoth.api.PlayerHandler
import org.jeudego.pairgoth.api.TournamentHandler
import org.jeudego.pairgoth.util.Colorizer.green
import org.jeudego.pairgoth.util.Colorizer.red
import org.jeudego.pairgoth.util.parse
import org.jeudego.pairgoth.util.toString
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApiServlet : HttpServlet() {

    public override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
        doRequest(request, response)
    }

    public override fun doPost(request: HttpServletRequest, response: HttpServletResponse) {
        doRequest(request, response)
    }

    public override fun doPut(request: HttpServletRequest, response: HttpServletResponse) {
        doRequest(request, response)
    }

    public override fun doDelete(request: HttpServletRequest, response: HttpServletResponse) {
        doRequest(request, response)
    }

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private fun doRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val requestLock = if (request.method == "GET") lock.readLock() else lock.writeLock()
        try {
            requestLock.lock()
            doProtectedRequest(request, response)
        } finally {
            requestLock.unlock()
        }
    }

    private fun doProtectedRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val uri = request.requestURI
        logger.logRequest(request, !uri.contains(".") && uri.length > 1)

        var payload: Json? = null
        var reason = "OK"
        try {

            // validate request

            if ("dev" == WebappManager.getProperty("webapp.env")) {
                response.addHeader("Access-Control-Allow-Origin", "*")
            }
            validateContentType(request)
            validateAccept(request);

            // parse request URI

            val parts = uri.split("/").filter { !it.isEmpty() }
            if (parts.size !in 2..5 || parts[0] != "api") throw ApiException(HttpServletResponse.SC_BAD_REQUEST)

            val entity = parts[1]
            val selector = parts.getOrNull(2)?.also { request.setAttribute(ApiHandler.SELECTOR_KEY, it) }
            val subEntity = parts.getOrNull(3)
            val subSelector = parts.getOrNull(4)?.also { request.setAttribute(ApiHandler.SUBSELECTOR_KEY, it) }

            // choose handler

            val handler = when (entity) {
                "tour" ->
                    when (subEntity) {
                        null -> TournamentHandler
                        "part" -> PlayerHandler
                        "pair" -> PairingHandler
                        else -> ApiHandler.badRequest("unknown sub-entity: $subEntity")
                    }
                "player" -> PlayerHandler
                else -> ApiHandler.badRequest("unknown entity: $entity")
            }

            // call handler

            payload = handler.route(request, response)
            // if payload is null, it means the handler already sent the response
            if (payload != null) {
                setContentType(response)
                payload.toString(response.writer)
            }

        } catch (apiException: ApiException) {
            reason = apiException.message ?: "unknown API error"
            if (reason == null) error(response, apiException.code) else error(
                request,
                response,
                apiException.code,
                reason,
                apiException
            )
        } catch (ioe: IOException) {
            logger.error(red("could not process call"), ioe)
            reason = ioe.message ?: "unknown i/o exception"
            error(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, reason, ioe)
        } finally {
            val builder = StringBuilder()
            builder.append(response.status).append(' ')
                .append(reason)
            if (response.status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                logger.info(red(">> {}"), builder.toString())
            } else {
                logger.info(green(">> {}"), builder.toString())
            }

            // CB TODO - should be bufferized and asynchronously written in synchronous chunks
            // so that header lines from parallel requests are not mixed up in the logs ;
            // synchronizing the whole request log is not desirable
            for (header in response.headerNames) {
                val value = response.getHeader(header)
                logger.trace(green(">>     {}: {}"), header, value)
            }
            if (payload != null) {
                try {
                    logger.logPayload(">>     ", payload, false)
                } catch (ioe: IOException) {
                }
            }
        }
    }

    @Throws(ApiException::class)
    protected fun validateContentType(request: HttpServletRequest) {
        // extract content type parts
        val contentType = request.contentType
        if (contentType == null) {
            if (request.method == "GET") return
            throw ApiException(
                HttpServletResponse.SC_BAD_REQUEST,
                "no content type header"
            )
        }
        val sep = contentType.indexOf(';')
        val mimeType: String
        var charset: String? = null
        if (sep == -1) mimeType = contentType else {
            mimeType = contentType.substring(0, sep).trim { it <= ' ' }
            val params =
                contentType.substring(sep + 1).split("=".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            if (params.size == 2 && params[0].lowercase(Locale.getDefault())
                    .trim { it <= ' ' } == "charset"
            ) {
                charset = params[1].lowercase(Locale.getDefault()).trim { it <= ' ' }
                    .replace("-".toRegex(), "")
            }
        }

        // check charset
        if (charset != null && EXPECTED_CHARSET != charset) throw ApiException(
            HttpServletResponse.SC_BAD_REQUEST,
            "UTF-8 content expected"
        )

        // check content type
        if (!isJson(mimeType)) throw ApiException(
            HttpServletResponse.SC_BAD_REQUEST,
            "JSON content expected"
        )

        // put Json body as request attribute
        try {
            Json.parse(request.reader)?.let { payload: Json ->
                request.setAttribute(ApiHandler.PAYLOAD_KEY, payload)
                if (logger.isInfoEnabled) {
                    logger.logPayload("<<     ", payload, true)
                }
            }
        } catch (ioe: IOException) {
            throw ApiException(HttpServletResponse.SC_BAD_REQUEST, ioe)
        }
    }

    @Throws(ApiException::class)
    protected fun validateAccept(request: HttpServletRequest) {
        val accept = request.getHeader("Accept")
            ?: throw ApiException(
                HttpServletResponse.SC_BAD_REQUEST,
                "Missing 'Accept' header"
            )
        if (!isJson(accept)) throw ApiException(
            HttpServletResponse.SC_BAD_REQUEST,
            "Invalid 'Accept' header"
        )
    }

    protected fun setContentType(response: HttpServletResponse) {
        response.contentType = "application/json; charset=UTF-8"
    }

    protected fun error(
        request: HttpServletRequest,
        response: HttpServletResponse,
        code: Int,
        message: String?,
        cause: Throwable? = null
    ) {
        try {
            if (code == 500) {
                logger.error(
                    "Request {} {} gave error {} {}",
                    request.method,
                    request.requestURI,
                    code,
                    message,
                    cause
                )
            }
            response.sendError(code, message)
        } catch (ioe: IOException) {
            logger.error("Could not send back error", ioe)
        }
    }

    protected fun error(response: HttpServletResponse, code: Int) {
        try {
            response.sendError(code)
        } catch (ioe: IOException) {
            logger.error("Could not send back error", ioe)
        }
    }

    companion object {
        private var logger = LoggerFactory.getLogger("api")
        private const val EXPECTED_CHARSET = "utf8"
        const val AUTH_HEADER = "Authorization"
        const val AUTH_PREFIX = "Bearer"
        private fun isJson(mimeType: String): Boolean {
            return "text/json" == mimeType || "application/json" == mimeType ||
                    mimeType.endsWith("+json")
        }
    }
}
