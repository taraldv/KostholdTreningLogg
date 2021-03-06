/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlet.statistikk;

import crypto.ValidSession;
import html.Div;
import html.Select;
import html.StandardHtml;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.http.Headers;
import util.sql.Database;

/**
 *
 * @author
 */
public class Kalender extends HttpServlet {

    PrintWriter out;// = resp.getWriter();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Headers.GET(resp);
        ValidSession.isValid(req, resp);
        /*PrintWriter*/ out = resp.getWriter();
        try {
            int brukerId = (int) req.getSession().getAttribute("brukerId");
            StandardHtml html = new StandardHtml("Statistikk Styrke");
            Select s = new Select("distinct year(dato) as dato from styrkeLogg order by dato desc", "kalenderYearSelect", "select");
            Calendar cal = new GregorianCalendar();
            Div div = new Div(s.toString() + getKalender(brukerId, cal.get(Calendar.YEAR)), "kalenderContainerDiv", "div-container");
            html.addBodyContent(div.toString());
            html.addBodyJS("attachServerRequestToSelect('getKalender','datoYear','kalenderYearSelect','statistikk/kalender/','kalenderContainerDiv','kalenderTableDiv')");
            out.print(html.toString());
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Headers.POST(response);
        PrintWriter out = response.getWriter();
        String type = request.getParameter("type");

        try {
            int brukerId = (int) request.getSession().getAttribute("brukerId");
            if (type.equals("getKalender")) {
                out.print(getKalender(brukerId, Integer.parseInt(request.getParameter("datoYear"))));
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    private Calendar[] parseKalenderData(String[][] styrkeDatoer, String[][] kondisjonDatoer) throws Exception {
        Calendar[] arr = new GregorianCalendar[styrkeDatoer.length + kondisjonDatoer.length];
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < styrkeDatoer.length; i++) {
            Calendar cal = new GregorianCalendar();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTime(df.parse(styrkeDatoer[i][0]));
            arr[i] = cal;
        }
        for (int j = 0; j < kondisjonDatoer.length; j++) {
            Calendar cal = new GregorianCalendar();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTime(df.parse(kondisjonDatoer[j][0]));
            arr[styrkeDatoer.length + j] = cal;
        }
        return arr;
    }

    private Calendar[] getCalendarArrayFromSQLData(String[][] sqlData) throws Exception {
        Calendar[] arr = new GregorianCalendar[sqlData.length];
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < sqlData.length; i++) {
            Calendar cal = new GregorianCalendar();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTime(df.parse(sqlData[i][0]));
            arr[i] = cal;
        }

        return arr;
    }

    private String getKalender(int brukerId, int year) throws Exception {
        String styrkeQuery = "SELECT DISTINCT dato FROM styrkeLogg WHERE brukerId = " + brukerId
                + " AND YEAR(dato) = ?;";
        //array med datoer '2019-01-04' [[dato][dato][dato]...]
        String[][] styrkeData = Database.multiQuery(styrkeQuery, new Object[]{year}).getData();
        String kondisjonQuery = "SELECT DISTINCT dato FROM kondisjonLogg WHERE brukerId = " + brukerId
                + " AND YEAR(dato) = ?;";
        String[][] kondisjonData = Database.multiQuery(kondisjonQuery, new Object[]{year}).getData();

        Calendar[] datoer = parseKalenderData(styrkeData, kondisjonData);
        int[] ukeTelling = countDatesInWeek(datoer);

        Calendar[] styrkeCalendarArray = getCalendarArrayFromSQLData(styrkeData);
        Calendar[] kondisCalendarArray = getCalendarArrayFromSQLData(kondisjonData);

        String kalenderTabell = buildKalenderBetter(ukeTelling, countDatesInWeek(styrkeCalendarArray), countDatesInWeek(kondisCalendarArray));

        //return Arrays.deepToString(data);
        return kalenderTabell;
    }

    private double gjennomsnitt(int[] arr) {
        double sum = 0;
        for (int i : arr) {
            sum += i;
        }
        sum = sum / arr.length;
        sum = sum * 100;
        int forkorting = (int) sum;
        return ((double) forkorting) / 100;
    }

    /*
    * Input is an Calendar array containing all dates which have been logged in a specific year
    * Output is an int array which has a count for every week.
    * Exmple: [0,0,0,0,2,3,4,5,0...]
    * The output should have a max length of current weeknumber, or 52
     */
    private int[] countDatesInWeek(Calendar[] arr) {
        int[] output = new int[52];
        Calendar cal = new GregorianCalendar();
        //If the dates belong to the current year, only count up to current weeknumber
        if (arr.length > 0 && arr[0].get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
            //Problems arise when it is december and week of year says 1 
            if (cal.get(Calendar.WEEK_OF_MONTH) != 5 || cal.get(Calendar.MONTH) != 11) {
                output = new int[cal.get(Calendar.WEEK_OF_YEAR)];
            }
        }
        for (int i = 0; i < arr.length; i++) {
            int week = arr[i].get(Calendar.WEEK_OF_YEAR);
            /*
            * Week 1 in late december should be put in week 52 not week 1.
            * This function does not have access to the next year, where it probably
            * should have been put
             */
            if (arr[i].get(Calendar.WEEK_OF_MONTH) == 5 && arr[i].get(Calendar.MONTH) == 11) {
                output[51]++;
            } else {
                output[week - 1]++;
            }
        }
        return output;
    }

    private String buildKalenderBetter(int[] arr, int[] styrkeArr, int[] kondisArr) {
        String infoString = "<p>Antall dager på trening i uka (gj.snitt): " + gjennomsnitt(styrkeArr) + "</p>";
        String infoString2 = "<p>Antall dager med jogging i uka (gj.snitt): " + gjennomsnitt(kondisArr) + "</p>";
        String tableString = "<table class='kalenderTable'>";
        int rows = 13;
        int cols = 4;
        for (int i = 0; i < rows; i++) {
            tableString += "<tr class='kalenderTableRow'>";
            for (int j = 0; j < cols; j++) {
                int week = ((i * cols) + (j + 1));
                tableString += "<td class='kalenderTableCell'>";
                try {
                    tableString += "<div class='color" + arr[week - 1] + "'>" + week + "</div>";
                } catch (ArrayIndexOutOfBoundsException e) {
                    tableString += "<div class='color0'>" + week + "</div>";
                }
                tableString += "</td>";
            }
            tableString += "</tr>";
        }
        return "<div id='kalenderTableDiv'>" + infoString2 + infoString + tableString + "</table></div>";
    }

    private String buildKalender(int[] arr, Calendar[] datoer) {
        String infoString = "<p>Antall dager på trening i uka (gj.snitt): " + gjennomsnitt(arr) + "</p>";
        String infoString2 = "<p>Antall dager på trening: " + datoer.length + "</p>";
        String tableString = "<table class='kalenderTable'>";
        int rows = 13;
        int cols = 4;
        for (int i = 0; i < rows; i++) {
            tableString += "<tr class='kalenderTableRow'>";
            for (int j = 0; j < cols; j++) {
                int week = ((i * cols) + (j + 1));
                tableString += "<td class='kalenderTableCell'>";
                tableString += "<div class='color" + arr[week - 1] + "'>" + week + "</div>";
                tableString += "</td>";
            }
            tableString += "</tr>";
        }
        return "<div id='kalenderTableDiv'>" + infoString2 + infoString + tableString + "</table></div>";
    }

}
