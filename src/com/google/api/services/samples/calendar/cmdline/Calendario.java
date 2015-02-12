/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.api.services.samples.calendar.cmdline;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.Lists;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import static com.google.api.services.samples.calendar.cmdline.View.display;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * @author Yaniv Inbar
 */
public class Calendario {

    /**
     * Be sure to specify the name of your application. If the application name
     * is {@code null} or blank, the application will log a warning. Suggested
     * format is "MyCompany-ProductName/1.0".
     */
    private static final String APPLICATION_NAME = "";

    /**
     * Directory to store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR
            = new java.io.File(System.getProperty("user.home"), ".store/calendar_sample");

    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to
     * make it a single globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static com.google.api.services.calendar.Calendar client;

    static final java.util.List<Calendar> addedCalendarsUsingBatch = Lists.newArrayList();

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static Credential authorize() throws Exception {
        File f = new File("client_secrets.json");
        System.out.println("" + f.getAbsolutePath());
        InputStream inputStream = new FileInputStream(f);
        InputStreamReader aux = new InputStreamReader(inputStream);

        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                aux);
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=calendar "
                    + "into calendar-cmdline-sample/src/main/resources/client_secrets.json");
            System.exit(1);
        }
        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public static void main(String[] args) {
        try {
            // Comprobación de argumentos
            if (args.length < 1) {
                mostrarInstrucciones();   
                System.exit(1);
            }
            // initialize the transport
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // initialize the data store factory
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

            // authorization
            Credential credential = authorize();

            // set up global Calendar instance
            client = new com.google.api.services.calendar.Calendar.Builder(
                    httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

            // Comprobamos entrada de datos modo depuracion
//            String aux[] = new String[3];
//            aux[0] = "add";
//            aux[1] = "qdqpan5f6f4u58vh2qbubqavm0@group.calendar.google.com";
////            aux[2] = "20150208-20:00";
////            aux[3] = "20150209-21:00";
//            aux[2] = "Evento nuevo";
            if (!entradaDatosValidos(args)) { //args)) {
                System.exit(1);
            }

            //addCalendarsUsingBatch();
            //Calendar calendar = addCalendar();
            //updateCalendar(calendar);      
            //showEvents(calendar);
            //deleteCalendarsUsingBatch();
            //deleteCalendar(calendar);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    private static boolean entradaDatosValidos(String[] args) throws IOException {

        String comando;

        comando = args[0];
        if (comando.equals("show")) {
            showCalendars(); // Ver calendarios disponibles
            return true;
        }
        if (comando.equals("add")) {
            // Seleccionar calendario
            if (args.length != 3) {
                System.out.println("Faltan o sobran parametros revise las instrucciones");
                return false;
            }
            Calendar calendario = getCalendar(args[1]); // ID del calendario
            if (calendario == null) {
                System.out.println("No se ha encontrado un calendario con ese ID");
                return false;
            }
            // Añadir evento start:fecha sistema y hora +5 ,minutos y end:start + 5
            java.util.Calendar aux = GregorianCalendar.getInstance();
            aux.roll(java.util.Calendar.MINUTE, 5); // Añadimos 5 minutos a la fecha sistema
            Date inicio = aux.getTime();//dameFecha(args[2].trim()); // Fecha inicio
            aux.roll(java.util.Calendar.MINUTE, 5); // Añadimos 5 minutos a la fecha sistema
            Date fin = aux.getTime();//dameFecha(args[3].trim()); // Fecha fin                        
            System.out.println("Fecha inicio: " + inicio);
            System.out.println("Fecha fin: " + fin);
            addEvent(calendario, inicio, fin, args[2]);
            return true;
        }
        mostrarInstrucciones();
        return false;

    }

    public static Date dameFecha(String aux) {
        try {
            String año = aux.substring(0, 4);
            String mes = aux.substring(4, 6);
            String dia = aux.substring(6, 8);
            String hora = aux.substring(9, 11);
            String minutos = aux.substring(12, 14);
            return new GregorianCalendar(Integer.parseInt(año), Integer.parseInt(mes) - 1, Integer.parseInt(dia), Integer.parseInt(hora), Integer.parseInt(minutos)).getTime();
        } catch (Exception e) {
            System.out.println("El Formato de fecha no es el adecuado");
            return null;
        }
    }

    public static void mostrarInstrucciones() {
        System.out.println("USO:");
        System.out.println("\tcalendario {show | add IDCalendario \"Asunto\"}");
        System.out.println("\nDONDE:\n");
        System.out.println("\tshow\tMuestra los ID de los calendarios");
        System.out.println("\tadd\tAñade un evento en calendario elegido en fecha y hora start y end");        
        System.out.println("\nOPCIONES:\n");
        System.out.println("\tIDCalendario\tIdentificador del calendario de google");        
        System.out.println("\tDescripcion\tTexto del evento. Entre Comiilas dobles");
        System.out.println("\tLa fecha de inicio sera la del sistema + 5 minutos.");
        System.out.println("\tLa fecha de fin sera la de start + 5 minutos más.");
    }

    private static void showCalendars() throws IOException {
        try {
            View.header("Show Calendars");
            com.google.api.services.calendar.Calendar.CalendarList aux = client.calendarList();
            com.google.api.services.calendar.Calendar.CalendarList.List aux2 = aux.list();
            CalendarList feed = aux2.execute();

            View.display(feed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addCalendarsUsingBatch() throws IOException {
        View.header("Add Calendars using Batch");
        BatchRequest batch = client.batch();

        // Create the callback.
        JsonBatchCallback<Calendar> callback = new JsonBatchCallback<Calendar>() {

            @Override
            public void onSuccess(Calendar calendar, HttpHeaders responseHeaders) {
                View.display(calendar);
                addedCalendarsUsingBatch.add(calendar);
            }

            @Override
            public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                System.out.println("Error Message: " + e.getMessage());
            }
        };

        // Create 2 Calendar Entries to insert.
        Calendar entry1 = new Calendar().setSummary("Calendar for Testing 1");
        client.calendars().insert(entry1).queue(batch, callback);

        Calendar entry2 = new Calendar().setSummary("Calendar for Testing 2");
        client.calendars().insert(entry2).queue(batch, callback);

        batch.execute();
    }

    private static Calendar addCalendar() throws IOException {
        View.header("Add Calendar");
        Calendar entry = new Calendar();
        entry.setSummary("Calendar for Testing 3");
        Calendar result = client.calendars().insert(entry).execute();
        View.display(result);
        return result;
    }

    private static Calendar updateCalendar(Calendar calendar) throws IOException {
        View.header("Update Calendar");
        Calendar entry = new Calendar();
        entry.setSummary("Updated Calendar for Testing");
        Calendar result = client.calendars().patch(calendar.getId(), entry).execute();
        View.display(result);
        return result;
    }

    private static void addEvent(Calendar calendar, Date start, Date end, String msg) throws IOException {
        View.header("Evento añadido!");
        Event event = newEvent(start, end, msg);
        Event result = client.events().insert(calendar.getId(), event).execute();
        View.display(result);
    }

    private static Event newEvent(Date pstart, Date pend, String pmsg) {
        Event event = new Event();
        event.setSummary(pmsg);
        Date startDate = pstart; //new Date();
        Date endDate = pend; //new Date(startDate.getTime() + 3600000);
        DateTime start = new DateTime(startDate, TimeZone.getTimeZone("UTC"));
        event.setStart(new EventDateTime().setDateTime(start));
        DateTime end = new DateTime(endDate, TimeZone.getTimeZone("UTC"));
        event.setEnd(new EventDateTime().setDateTime(end));
        return event;
    }

    private static void showEvents(Calendar calendar) throws IOException {
        View.header("Show Events");
        Events feed = client.events().list(calendar.getId()).execute();
        View.display(feed);
    }

    private static void deleteCalendarsUsingBatch() throws IOException {
        View.header("Delete Calendars Using Batch");
        BatchRequest batch = client.batch();
        for (Calendar calendar : addedCalendarsUsingBatch) {
            client.calendars().delete(calendar.getId()).queue(batch, new JsonBatchCallback<Void>() {

                @Override
                public void onSuccess(Void content, HttpHeaders responseHeaders) {
                    System.out.println("Delete is successful!");
                }

                @Override
                public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                    System.out.println("Error Message: " + e.getMessage());
                }
            });
        }

        batch.execute();
    }

    private static void deleteCalendar(Calendar calendar) throws IOException {
        View.header("Delete Calendar");
        client.calendars().delete(calendar.getId()).execute();
    }

    private static Calendar getCalendar(String id) throws IOException {

        CalendarList feed = client.calendarList().list().execute();
        if (feed.getItems() != null) {
            for (CalendarListEntry entry : feed.getItems()) {
                if (entry.getId().equals(id)) {
                    Calendar aux = client.calendars().get(id).execute();
                    return aux;
                }
            }
        }
        return null;
    }

}
