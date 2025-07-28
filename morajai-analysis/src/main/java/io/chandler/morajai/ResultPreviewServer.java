package io.chandler.morajai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ResultPreviewServer extends HttpServlet {

    private Server server;

    private static final String MORAJAI_HTML = "/morajai.html";
    private static final String MORAJAI_JS = "/js/morajai-bundle.min.js";

    public void start() throws Exception {
        this.server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.setConnectors(new Connector[] { connector });
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new ResultPreviewServlet()), "/*");
        
        server.setHandler(context);
        server.start();
        server.join();
    }

    public static void main(String[] args) throws Exception {
        // CHeck for morajai.html
        if (ResultPreviewServlet.class.getResource(MORAJAI_HTML) == null) {
            System.out.println("morajai.html not found");
            System.exit(1);
        }
        if (MoraJaiSimulator.class.getResource(MORAJAI_JS) == null) {
            System.out.println("morajai-bundle.min.js not found");
            System.exit(1);
        }

        System.out.println("Starting ResultPreviewServer on port 8080");
        System.out.println("Open your browser to http://localhost:8080");
        new ResultPreviewServer().start();
    }

    public static class ResultPreviewServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String path = req.getPathInfo();
            if (path == null || "/".equals(path) || "/index.html".equals(path)) {
                path = MORAJAI_HTML;
            }
            InputStream in;
            if (path.equals(MORAJAI_JS)) {
                resp.setContentType("application/javascript; charset=UTF-8");
                in = MoraJaiSimulator.class.getResourceAsStream(MORAJAI_JS);
            } else if (path.equals(MORAJAI_HTML)) {
                resp.setContentType("text/html; charset=UTF-8");
                in = ResultPreviewServlet.class.getResourceAsStream(MORAJAI_HTML);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().println("404 Not Found: " + path);
                return;
            }

            // Stream the file
            OutputStream out = resp.getOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.flush();
        }
    
    }

}