package ca.umontreal.iro.hurtubin.permissions;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Référence : https://inthecheesefactory.com/blog/things-you-need-to-know-about-android-m-permission-developer-edition/en
 */
public class MainActivity extends AppCompatActivity {

    final private int REQUEST_CODE_DISPLAY_LAST_CALL = 123;
    final private int REQUEST_CODE_INSERT_CONTACT = 124;

    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.button);
        Button contactBtn = findViewById(R.id.contactBtn);

        text = findViewById(R.id.text);

        btn.setOnClickListener(v -> displayLastCallWrapper());

        contactBtn.setOnClickListener(v -> insertDummyContactWrapper());
    }

    private void displayLastCall() {

        String lastCall = CallLog.Calls.getLastOutgoingCall(this);

        text.setText(lastCall);
    }

    private void insertDummyContact() {
        // Two operations are needed to insert a new contact.
        ArrayList<ContentProviderOperation> operations = new
                ArrayList<>(2);

        // First, set up a new raw contact.
        ContentProviderOperation.Builder op = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null);

        operations.add(op.build());

        // Next, set the name for the contact.
        op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        "__ Contact ajouté par l'application de test");
        operations.add(op.build());

        // Apply the operations.
        ContentResolver resolver = getContentResolver();

        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operations);
        } catch (OperationApplicationException | RemoteException e) {
            Log.d("ERR", "Could not add a new contact:" + e.getMessage());
        }
    }

    /**
     * Wrapper pour appeler displayLastCall seulement lorsque les permissions le permettent
     */
    private void displayLastCallWrapper() {

        int hasPermission = checkSelfPermission(Manifest.permission.READ_CALL_LOG);

        if (hasPermission != PackageManager.PERMISSION_GRANTED) {

            /*
              Affichage d'un popup, pour donner un certain feedback
              au cas où l'utilisateur coche "Never ask again"
             */
            new AlertDialog.Builder(this)
                    .setMessage("Afin d'afficher le denier appel effectué, vous devez autoriser l'application à lire le log d'appels")
                    .setPositiveButton("Ok", (dialog, which) -> {
                        // Demande la permission voulue
                        requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CODE_DISPLAY_LAST_CALL);
                    })
                    .setNegativeButton("Annuler", null)
                    .show();

            return;
        }

        // Si on a déjà la permission, on peut directement appeler la displayLastCall
        displayLastCall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_DISPLAY_LAST_CALL:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    displayLastCall();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Désolé, impossible d'afficher le dernier appel", Toast.LENGTH_SHORT).show();
                }
                break;

            case REQUEST_CODE_INSERT_CONTACT: {
                Map<String, Integer> perms = new HashMap<>();

                // Permissions demandées
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_CONTACTS, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_CONTACTS, PackageManager.PERMISSION_GRANTED);

                // Résultats (Granted/Denied)
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                    Log.i("wat", permissions[i] + " ;: " + grantResults[i]);
                }

                // Vérifie que les permissions ont été accordées

                if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {

                    // All Permissions Granted
                    insertDummyContact();

                } else {
                    // Some permission Denied
                    Toast.makeText(MainActivity.this, "Certaines fonctionnalités de l'application pourraient ne pas fonctionner correctement. Assurez-vous d'accorder les permissions nécessaires pour y avoir accès.", Toast.LENGTH_SHORT).show();
                }
            }
            break;


            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }


    private void insertDummyContactWrapper() {
        List<String> permissionsNeeded = new ArrayList<>();
        final List<String> permissionsList = new ArrayList<>();

        if (addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("GPS");
        if (addPermission(permissionsList, Manifest.permission.READ_CONTACTS))
            permissionsNeeded.add("Read Contacts");
        if (addPermission(permissionsList, Manifest.permission.WRITE_CONTACTS))
            permissionsNeeded.add("Write Contacts");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                StringBuilder message = new StringBuilder("Pour insérer un contact dans la base de données, vous devez accorder les permissions suivantes : " + permissionsNeeded.get(0));

                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message.append(", ").append(permissionsNeeded.get(i));

                new AlertDialog.Builder(this)
                        .setMessage(message.toString())
                        .setPositiveButton("Ok", (dialog, which) -> {
                            // Demande les permissions requises
                            requestPermissions(permissionsList.toArray(new String[0]), REQUEST_CODE_INSERT_CONTACT);
                        })
                        .setNegativeButton("Annuler", null)
                        .show();

                return;
            }

            return;
        }

        insertDummyContact();
    }

    private boolean addPermission(List<String> permissionsList, String permission) {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);

            // Check for Rationale Option
            return !shouldShowRequestPermissionRationale(permission);
        }

        return false;
    }
}
