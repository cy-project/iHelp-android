package edu.stu.ihelp.client;

import edu.stu.ihelp.client.PersonalData.ContactList.ViewHolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PersonalData extends Activity {

    private EditText et_search, et_name;
    private Button confirm;
    private TextView contactCount;
    private SharedPreferences spfs;

    private ContentResolver resolver;

    private ContactList adapter;
    private ListView listview;
    private Cursor contacts_number;
    private Map<String, String> contactsMap;
    private List<Map<String, String>> contactsArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.personal_data);

        et_name = (EditText) findViewById(R.id.name);
        et_search = (EditText) findViewById(R.id.searchName);
        listview = (ListView) findViewById(R.id.contact_list);
        confirm = (Button) findViewById(R.id.btn_submit);
        contactCount = (TextView) findViewById(R.id.contact_count);
        spfs = getSharedPreferences("PersonalData", 0);

        et_name.setText(Variable.name);

        resolver = getContentResolver();

        getPhoneBookData();

        contactCount.setText(Variable.contactsPhone.size() + "");
        adapter = new ContactList(getLayoutInflater(), contactsArrayList);

        listview.setAdapter(adapter);

        et_name.setOnEditorActionListener(new OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {

                return event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            }
        });

        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int start, int before,
                    int count) {
                adapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        listview.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
                ViewHolder holder = (ViewHolder) arg1.getTag();
                holder.selected.toggle();

                String name = holder.name.getText().toString();
                String phone = holder.phone.getText().toString();
                if (holder.selected.isChecked()) {
                    Variable.contactsPhone.add(phone);
                } else {
                    Variable.contactsPhone.remove(phone);
                }
                contactCount.setText(Variable.contactsPhone.size() + "");

                Log.e("CheckBox clicked",
                        arg2 + "   " + holder.selected.isChecked() + name);
                adapter.setStatus(name, holder.selected.isChecked());
            }
        });

        confirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter.getList().size() > 0) {
                    Variable.setData(PersonalData.this, adapter.getList());
                }

                if (et_name.getText().toString().equals("")) {
                    Toast.makeText(PersonalData.this, "建議您輸入姓名",
                            Toast.LENGTH_SHORT).show();
                }

                spfs.edit().putString("UserName", et_name.getText().toString())
                        .commit();

                String contactsPhone = "";
                Iterator<String> iterator = Variable.contactsPhone.iterator();
                while (iterator.hasNext()) {
                    contactsPhone += iterator.next() + ",";
                }

                spfs.edit().putString("contacts", contactsPhone).commit();

                Variable.name = et_name.getText().toString();

                setResult(RESULT_OK);
                Toast.makeText(PersonalData.this, "儲存成功", Toast.LENGTH_SHORT)
                        .show();

                Builder alerDialog = new AlertDialog.Builder(PersonalData.this);
                alerDialog.setTitle("通知緊急聯絡人");
                alerDialog.setMessage("是否要發簡訊通知緊急聯絡人已經成為 iHELP 通知對象呢？");
                alerDialog.setPositiveButton("好",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {

                                for (String phone : Variable.contactsPhone) {
                                    if (phone.equals("")) {
                                        continue;
                                    }
                                    sendSMS(phone.replaceAll("\\s+", ""),
                                            "我已經將您設定為 iHelp 緊急聯絡人。");
                                }
                                
                                PersonalData.this.finish();
                            }
                        });

                alerDialog.setNegativeButton("不用",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                PersonalData.this.finish();
                            }
                        });
                
                alerDialog.show();

            }
        });

    }

    private void sendSMS(String phone, String text) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> messageArray = smsManager.divideMessage(text);
        smsManager.sendMultipartTextMessage(phone, null, messageArray, null,
                null);
    }

    private void getPhoneBookData() {
        contactsArrayList = new ArrayList<Map<String, String>>();
        contacts_number = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
                null, null);
        String name, phone;
        while (contacts_number.moveToNext()) {

            name = contacts_number.getString(contacts_number
                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

            phone = contacts_number
                    .getString(
                            contacts_number
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    .replaceAll("\\s", "");

            if (phone.equals("")) {
                continue;
            }
            if (!phone.matches("^(09|\\+886).*")) {
                continue;
            }

            contactsMap = new HashMap<String, String>();
            contactsMap.put(Variable.CONTACT_NAME, name);
            contactsMap.put(Variable.CONTACT_PHONE, phone);
            contactsArrayList.add(contactsMap);

        }
    }

    class ContactList extends BaseAdapter implements Filterable {
        LayoutInflater inflater;
        Map<String, Boolean> btnStatus;
        List<Map<String, String>> show;
        List<Map<String, String>> origin;

        ContactList(LayoutInflater inflat, List<Map<String, String>> list) {
            this.inflater = inflat;
            this.show = list;
            this.origin = list;

            btnStatus = new HashMap<String, Boolean>();
            for (int i = 0; i < show.size(); i++) {
                String name = show.get(i).get(Variable.CONTACT_NAME);
                btnStatus.put(name, false);
            }
        }

        public boolean getStatus(String userName) {
            return btnStatus.get(userName);
        }

        public void setStatus(String userName, boolean bn) {
            btnStatus.remove(userName);
            btnStatus.put(userName, bn);
        }

        @Override
        public int getCount() {
            return show.size();
        }

        @Override
        public Object getItem(int position) {
            return show.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;
            if (view == null) {
                holder = new ViewHolder();
                view = inflater.inflate(R.layout.people, parent, false);
                holder.name = (TextView) view.findViewById(R.id.contact_name);
                holder.phone = (TextView) view.findViewById(R.id.contact_phone);
                holder.selected = (CheckBox) view.findViewById(R.id.select);

                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            String name = show.get(position).get(Variable.CONTACT_NAME);
            String phone = show.get(position).get(Variable.CONTACT_PHONE);
            holder.name.setText(name);
            holder.phone.setText(phone);
            holder.selected.setChecked(Variable.contactsPhone.contains(phone));

            return view;
        }

        class ViewHolder {
            TextView name;
            TextView phone;
            CheckBox selected;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence constraint,
                        FilterResults results) {
                    Log.i("Filter", "publishResult");
                    show = (List<Map<String, String>>) results.values;
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    Log.e("Filter", "performFiltering");
                    FilterResults results = new FilterResults();
                    List<Map<String, String>> FilteredList = new ArrayList<Map<String, String>>();
                    if (constraint == null || constraint.length() == 0) {
                        Log.e("constraint", "==null");
                        // No filter implemented we return all the list
                        results.values = origin;
                        results.count = origin.size();
                        Log.e("Size", results.count + "");
                    } else {
                        Log.e("constraint", "!=null");
                        for (int i = 0; i < show.size(); i++) {
                            Map<String, String> data = show.get(i);
                            if (data.get(Variable.CONTACT_NAME).toLowerCase()
                                    .contains(constraint.toString())) {
                                FilteredList.add(data);
                            }
                        }
                        results.values = FilteredList;
                        results.count = FilteredList.size();
                        Log.e("Size", results.count + "");
                    }
                    return results;
                }
            };
            return filter;
        }

        List<Map<String, String>> getList() {
            List<Map<String, String>> result = new ArrayList<Map<String, String>>();

            for (int i = 0; i < origin.size(); i++) {
                String name = origin.get(i).get(Variable.CONTACT_NAME);
                Log.e("name", name);
                boolean status = btnStatus.get(name);
                if (status)
                    result.add(origin.get(i));
            }

            for (int i = 0; i < result.size(); i++) {
                Log.e("name:" + result.get(i).get(Variable.CONTACT_NAME),
                        "phone:" + result.get(i).get(Variable.CONTACT_PHONE));
            }

            return result;
        }

    }

}
