/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet;

import crypto.SessionLogin;
import html.IndexHtml;
import html.Input;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.http.Headers;
import util.mail.SendMail;
import util.sql.Database;

/**
 *
 * @author
 */
public class NyBruker extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Headers.GET(response);
        PrintWriter out = response.getWriter();
        String msg = request.getParameter("msg");
        try {
            IndexHtml html = new IndexHtml("LoggLogg Ny Bruker");
            Input navn = new Input("skriv inn epost her", "epost", "text", "brukernavnInput", "input-login", "epost", "on");
            Input passord = new Input("skriv inn passord her", "passord", "password", "passordInput", "input-login", "passord", "on");
            String properSubmit = "<input id='loginSubmitInput' class='input-login' type='submit' value='registrer'>";
            String properForm = "<form id='registrerForm' class='form-login' method='POST' action=''>"
                    + navn.toString()
                    + passord.toString()
                    + properSubmit
                    + "</form>";
            html.addBodyContent(properForm);
            if (msg != null) {
                html.addBodyContent("<h3>Epost sendt, sjekk innboks eller spam</h3>");
            }

            out.print(html);
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Headers.POST(response);
        PrintWriter out = response.getWriter();
        try {
            String epost = request.getParameter("epost");
            String escapedEpost = escape(epost);
            String pw = request.getParameter("passord");
            //tror ikke nyBruker kan bli noe annet en 1 eller exception
            int brukerId = nyBruker(escapedEpost, pw);
            SendMail sm = new SendMail(4, escapedEpost, brukerId,
                    "Aktiver din bruker på logglogg.no",
                    "Klikk her for å aktivere din bruker",
                    "Hei, din epost har blitt brukt til å lage en konto på logglogg.no",
                    "https://logglogg.no/aktiverepost/");
            sm.send();
            response.sendRedirect("/nybruker/?msg=sendt");

        } catch (Exception e) {
            response.sendRedirect("https://logglogg.no/nybruker?error=1");
            // e.printStackTrace(out);
        }
    }

    private int nyBruker(String epost, String passord) throws Exception {
        String query = "INSERT INTO users(brukernavn,passord) VALUES (?,?);";
        return Database.singleUpdateQuery(query, new Object[]{epost, SessionLogin.generatePasswordHash(passord)}, true);
    }

    private String escape(String epost) {
        String removeSemicolon = epost.replaceAll(";", "");
        String removeHyp = removeSemicolon.replaceAll("'", "");
        String removeComma = removeHyp.replaceAll(",", "");
        return removeComma.replaceAll("\"", "");
    }

}
