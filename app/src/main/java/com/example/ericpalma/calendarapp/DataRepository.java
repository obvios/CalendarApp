package com.example.ericpalma.calendarapp;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import static android.content.ContentValues.TAG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DataRepository implements AsyncResponse{
    private AccountsDao accountsDao;
    private AppointmentsDao appointmentsDao;
    private HashMap<String,Appointments> allAppointmentsMap;
    private HashSet<String> allAccountsSet;

    DataRepository(Application application){
        CalendarAppDatabase db = CalendarAppDatabase.getINSTANCE(application);
        accountsDao = db.accountsDao();
        appointmentsDao = db.appointmentsDao();
        allAppointmentsMap = new HashMap<>();
        allAccountsSet = new HashSet<>();
        new mapAllAccountsTask(accountsDao).execute(allAccountsSet);
        new mapAllAppointmentsTask(appointmentsDao).execute(allAppointmentsMap);
    }


    public void insertAccount(Accounts account){
        allAccountsSet.add(account.getUsername());
        new insertAccountTask(accountsDao).execute(account);
    }

    public void changeUsername(String oldUsername, String newUsername, String password){
        new changeUsernameTask(oldUsername, newUsername, password, accountsDao).execute();
    }

    public void changePassword(String username, String currentPassword, String newPassword){
        new changePasswordTask(username,currentPassword,newPassword,accountsDao).execute();
    }

    public void changeCalendarTypePreference(String username, String password, String newViewType){
        new changeCalendarTypePreferenceTask(username,password,newViewType,accountsDao).execute();
    }

    public void modifyAccountData(String username, String password, String newFirst, String newLast){
        new modifyAccountDataTask(username,password,newFirst,newLast,accountsDao).execute();
    }

    public Boolean checkAvailabitity(String date){
        return !allAppointmentsMap.containsKey(date);
    }

    public void getAccountAppointments(String username,AsyncResponse listener){
        new getAccountAppointmentsTask(accountsDao,listener).execute(username);
    }

    public void insertAppointment(Appointments appointment){
        new insertAppointmentTask(appointmentsDao).execute(appointment);
        updateAppointmentsMap(appointment);
    }

    public void deleteAccount(Accounts account){
        allAccountsSet.remove(account.getUsername());
        new deleteAccountTask(accountsDao).execute(account);
    }

    private void updateAppointmentsMap(Appointments appointment){
        String date_timeKey = appointment.getDate() + " " + appointment.getTime();
        allAppointmentsMap.put(date_timeKey,appointment);
    }

    public void deleteAppointment(String appointmentDateTime){
        Appointments appointment = allAppointmentsMap.get(appointmentDateTime);
        allAppointmentsMap.remove(appointmentDateTime);
        new deleteAppointmentTask(appointmentsDao).execute(appointment);
    }


    public void changeAppointment(String appointmentDateTime, String newDate, String newTime) {
        String newKey = newDate + " " + newTime;
        allAppointmentsMap.put(newKey, allAppointmentsMap.get(appointmentDateTime));
        new changeAppointmentTask(newDate, newTime, appointmentsDao).execute(allAppointmentsMap.get(appointmentDateTime));
        allAppointmentsMap.remove(appointmentDateTime);
    }

    public Boolean accountIsCreated(String username){
        return allAccountsSet.contains(username);
    }

    public void downloadAppointments(String username){
        new getAccountAppointmentsTask(accountsDao,this).execute(username);
    }

    /*Perform all functions on a separate thread.
      Below are the async functions.
    */

    /*get appointments in background*/
    private static class getAccountAppointmentsTask extends AsyncTask<String, Void, List<Appointments>> {
        private AccountsDao asyncAccountsDao;
        private AsyncResponse delegate;

        getAccountAppointmentsTask(AccountsDao dao,AsyncResponse delegate){
            asyncAccountsDao = dao;
            this.delegate = delegate;
        }

        @Override
        protected List<Appointments> doInBackground(String... strings) {
            return asyncAccountsDao.getAccountAppointments(strings[0]);
        }

        @Override
        protected void onPostExecute(List<Appointments> appointments) {
            delegate.onAccountAppointmentsRetrieved(appointments);
        }
    }

    /*insert account in background*/
    private static class insertAccountTask extends AsyncTask<Accounts, Void, Void>{

        private AccountsDao asyncAccountsDao;

        insertAccountTask(AccountsDao dao){
            asyncAccountsDao = dao;
        }

        @Override
        protected Void doInBackground(Accounts... account) {
            asyncAccountsDao.insertUserAccount(account[0]);
            return null;

        }
    }

    /*Change username in background*/
    private static class changeUsernameTask extends AsyncTask<String,Void,Void>{
        private AccountsDao asyncAccountsDao;
        private String oldUsername;
        private String newUsername;
        private String password;

        changeUsernameTask(String oldUsername, String newUsername, String password ,AccountsDao dao){
            asyncAccountsDao = dao;
            this.oldUsername = oldUsername;
            this.newUsername = newUsername;
            this.password = password;
        }

        @Override
        protected Void doInBackground(String... strings) {
            asyncAccountsDao.changeUsername(oldUsername,newUsername,password);
            return null;
        }
    }

    /*Change password in background*/
    private static class changePasswordTask extends AsyncTask<String,Void,Void>{
        private AccountsDao asyncAccountsDao;
        private String username;
        private String currentPassword;
        private String newPassword;

        changePasswordTask(String username, String currentPassword, String newPassword, AccountsDao dao){
            asyncAccountsDao = dao;
            this.username = username;
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }

        @Override
        protected Void doInBackground(String... strings) {
            asyncAccountsDao.changePassword(username, currentPassword,newPassword);
            return null;
        }
    }

    /*Change calendar view type in background*/
    private static class changeCalendarTypePreferenceTask extends AsyncTask<String,Void,Void>{
        private AccountsDao asyncAccountsDao;
        private String username;
        private String password;
        private String type;

        changeCalendarTypePreferenceTask(String username, String password, String type, AccountsDao dao){
            asyncAccountsDao = dao;
            this.username = username;
            this.password = password;
            this.type = type;
        }

        @Override
        protected Void doInBackground(String... strings) {
            asyncAccountsDao.changeCalTypePreference(username,password,type);
            return null;
        }
    }

    private static class modifyAccountDataTask extends AsyncTask<String,Void,Void>{
        private AccountsDao asyncAccountsDao;
        private String username;
        private String password;
        private String newFirstName;
        private String newLastName;

        modifyAccountDataTask(String username, String password, String newFirstName, String newLastName, AccountsDao dao){
            this.asyncAccountsDao = dao;
            this.username = username;
            this.password = password;
            this.newFirstName = newFirstName;
            this.newLastName = newLastName;
        }

        @Override
        protected Void doInBackground(String... strings) {
            asyncAccountsDao.modifyAccountData(username,password,newFirstName,newLastName);
            return null;
        }
    }

    /*insert appointment in background*/
    private static class insertAppointmentTask extends AsyncTask<Appointments, Void, Void>{
        private AppointmentsDao asyncAppointmentsDao;

        insertAppointmentTask(AppointmentsDao dao){
            asyncAppointmentsDao = dao;
        }

        @Override
        protected Void doInBackground(Appointments... appointment) {
            asyncAppointmentsDao.insertAppointment(appointment[0]);
            return null;
        }
    }

    /*delete account in background*/
    private static class deleteAccountTask extends AsyncTask<Accounts,Void,Void>{
        private AccountsDao asyncAppointmentsDao;

        deleteAccountTask(AccountsDao dao){
            asyncAppointmentsDao = dao;
        }


        @Override
        protected Void doInBackground(Accounts... account) {
            asyncAppointmentsDao.deleteAccount(account[0]);
            return null;
        }
    }

    /*delete appointment in background*/
    private static class deleteAppointmentTask extends AsyncTask<Appointments,Void,Void>{
        private AppointmentsDao asyncAppointmentsDao;

        deleteAppointmentTask(AppointmentsDao dao){
            asyncAppointmentsDao = dao;
        }

        @Override
        protected Void doInBackground(Appointments... appointment) {
            asyncAppointmentsDao.deleteAppointment(appointment[0]);
            return null;
        }
    }

    private static class changeAppointmentTask extends AsyncTask<Appointments,Void,Void>{
        private AppointmentsDao asyncAppointmentsDao;
        private String newDate;
        private String newTime;

        changeAppointmentTask(String newDate, String newTime, AppointmentsDao dao){
            this.asyncAppointmentsDao = dao;
            this.newDate = newDate;
            this.newTime = newTime;
        }

        @Override
        protected Void doInBackground(Appointments... appointments) {
            asyncAppointmentsDao.changeAppointmentDayTime(appointments[0].getDate(),appointments[0].getTime(),newDate,newTime);
            return null;
        }
    }

    private static class mapAllAppointmentsTask extends AsyncTask<HashMap<String,Appointments>,Void,Void>{
        /*Map all appoinments based on date and time only*/
        private AppointmentsDao appointmentsDao;

        mapAllAppointmentsTask(AppointmentsDao dao){
            this.appointmentsDao = dao;
        }

        @Override
        protected Void doInBackground(HashMap<String,Appointments>... maps) {
            List<Appointments> allAppointments = appointmentsDao.getAllAppointments();
            for( Appointments appointment : allAppointments){
                String Date_And_Time = appointment.getDate() + " " + appointment.getTime();
                maps[0].put(Date_And_Time,appointment);
            }
            return null;
        }
    }

    private static class mapAllAccountsTask extends AsyncTask<HashSet<String>,Void,Void>{
        private AccountsDao accountsDao;

        mapAllAccountsTask(AccountsDao dao){
            this.accountsDao = dao;
        }

        @Override
        protected Void doInBackground(HashSet<String>... sets) {
            List<Accounts> allAccounts = accountsDao.getAllAccounts();
            for (Accounts account: allAccounts){
                sets[0].add(account.getUsername());
            }
            return null;
        }
    }
    @Override
    public void onAccountAppointmentsRetrieved(List<Appointments> appointmentsList) {
        try {
            File root = Environment.getExternalStorageDirectory();
            File dir = new File (root.getAbsolutePath() + "/download");
            File file = new File(dir,  appointmentsList.get(0).getApptUserName() +".txt");

            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            for(Appointments appt: appointmentsList){
                pw.println(appt.getDate() + " " + appt.getTime());
            }
            pw.flush();
            pw.close();
            f.close();

        }catch (IOException e){
            Log.d(TAG,"failed");
        }
        return;
    }

}
