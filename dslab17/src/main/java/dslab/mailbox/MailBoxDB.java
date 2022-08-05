package dslab.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MailBoxDB {

    private int last_id = 0;
    private List<Mail> mails;

    public static class Mail {
        public int id;
        public String from, to, account, subject, data, hash;
        public Mail(int id, String from, String to, String account, String subject, String data, String hash) {
            this(from, to, account, subject, data, hash);
            this.id = id;
        }

        public Mail(String from, String to, String account, String subject, String data, String hash) {
            this.from = from;
            this.to = to;
            this.account = account;
            this.subject = subject;
            this.data = data;
            this.hash = hash;
        }
    }

    public MailBoxDB(){
        //mails = new ArrayList<>();
        mails = Collections.synchronizedList(new ArrayList<Mail>());
    }

    public synchronized void add(String from, String to, String account, String subject, String data, String hash){
        mails.add(new Mail(++last_id, from, to, account,  subject, data, hash));
    }

    public void remove(int id){
        mails.remove(getByID(id));
    }

    public Mail getByID(int id){
        for (Mail mail : mails){
            if(mail.id == id){
                return mail;
            }
        }
        return null;
    }

    public List<Mail> getAll(){
        return mails;
    }

    public ArrayList<Mail> getByAccount(String account){
        ArrayList<Mail> list = new ArrayList<>();
        for (Mail mail : mails){
            if(mail.account.equals(account)){
                list.add(mail);
            }
        }
        return list;
    }
}
