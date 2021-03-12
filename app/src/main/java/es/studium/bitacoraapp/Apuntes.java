package es.studium.bitacoraapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

public class Apuntes extends AppCompatActivity
{
    //modificar en caso de que cambie la ip del equipo
    String ip = "192.168.1.106";

    ListView listaApuntes;
    ArrayList<String> apuntes;
    String cuadernoSeleccionado;

    ConsultaRemota acceso;
    AltaRemota alta;
    BajaRemota baja;
    ModificacionRemota modifica;

    JSONArray result;
    JSONObject jsonobject;
    int posicion;
    ArrayAdapter<String> adapter;

    Button btnVolver;
    FloatingActionButton fabApuntes;
    String[] cadena;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apuntes);

        TextView cuaderno = findViewById(R.id.txtCuadernoSeleccionado);

        // Recibimos el cuaderno seleccionado
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        cuadernoSeleccionado = bundle.getString("cuaderno");

        // Separamos la cadena, el id y el nombre
        cadena = cuadernoSeleccionado.split("  -  ");
        cuaderno.setText("Bitácora " + "-" + cadena[1] + "-");

        listaApuntes = findViewById(R.id.listaApuntes);
        btnVolver = findViewById(R.id.btnVolver);
        fabApuntes = findViewById(R.id.fabApuntes);

        apuntes = new ArrayList<>();

        // Creamos el adaptador
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, apuntes);

        // Asignamos el adaptador a nuestro ListView
        listaApuntes.setAdapter(adapter);

        // Alta de un apunte
        fabApuntes.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(Apuntes.this);
                alertDialog.setTitle("NUEVO APUNTE");
                alertDialog.setMessage("Introduzca los datos");
                EditText fechaApunte = new EditText(Apuntes.this);
                fechaApunte.setHint("Fecha");
                EditText textoApunte = new EditText(Apuntes.this);
                textoApunte.setHint("comentario");
                LinearLayout layout = new LinearLayout(Apuntes.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(fechaApunte);
                layout.addView(textoApunte);
                alertDialog.setView(layout);
                // Botón Confirmar nuevo apunte
                alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        alta = new AltaRemota(fechaApunte.getText().toString(), textoApunte.getText().toString(), cadena[0]);
                        alta.execute();
                        acceso = new ConsultaRemota(cadena[0]);
                        acceso.execute();
                    }
                });
                alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dialog.cancel();
                    }
                });
                alertDialog.show();
            }
        });

        // Botón Modificar
        listaApuntes.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] apunteSeleccionado = listaApuntes.getItemAtPosition(position).toString().split("-");
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(Apuntes.this);
                alertDialog.setTitle("MODIFICAR APUNTE");
                alertDialog.setMessage("Modifica apunte");
                EditText fechaApunte = new EditText(Apuntes.this);
                fechaApunte.setText(apunteSeleccionado[1] + "/" + apunteSeleccionado[2] + "/" + apunteSeleccionado[3]);
                EditText textoApunte = new EditText(Apuntes.this);
                textoApunte.setText(apunteSeleccionado[4]);
                LinearLayout layout = new LinearLayout(Apuntes.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.addView(fechaApunte);
                layout.addView(textoApunte);
                alertDialog.setView(layout);
                // Botón Confirmar
                alertDialog.setPositiveButton("Confirmar", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        modifica = new ModificacionRemota(apunteSeleccionado[0], fechaApunte.getText().toString(), textoApunte.getText().toString());
                        modifica.execute();
                        acceso = new ConsultaRemota(cadena[0]);
                        acceso.execute();
                    }
                });
                // Botón Cancelar
                alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dialog.cancel();
                    }
                });
                alertDialog.show();
            }
        });
        acceso = new ConsultaRemota(cadena[0]);
        acceso.execute();

        // Baja de un apunte
        listaApuntes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> arg0, View v, int index, long arg3)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(Apuntes.this);
                builder.setMessage("Confirma si quieres eliminar el apunte")
                        .setCancelable(false)
                        .setPositiveButton("Confirmar", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id)
                            {
                                // Consigo el ID del cuaderno seleccionado de la lista
                                String[] apunteSeleccionado = listaApuntes.getItemAtPosition(index).toString().split("  -  ");
                                baja = new BajaRemota(apunteSeleccionado[0]);
                                baja.execute();
                                acceso = new ConsultaRemota(cadena[0]);
                                acceso.execute();
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });
        btnVolver.setOnClickListener(v -> onBackPressed());
    }

    // Consulta apuntes
    private class ConsultaRemota extends AsyncTask<Void, Void, String>
    {
        // Atributos
        String idCuadernoFK;
        // Constructor
        public ConsultaRemota(String id)
        {
            this.idCuadernoFK = id;
        }
        // Inspectores
        protected void onPreExecute()
        {
        }
        protected String doInBackground(Void... argumentos)
        {
            try
            {
                // Crear la URL de conexión al API
                Uri uri = new Uri.Builder().scheme("http").authority(ip).path("/ApiRest/apuntes.php").appendQueryParameter("idCuaderno", this.idCuadernoFK).build();
                // Create connection
                URL url = new URL(uri.toString());
                // Crear la conexión HTTP
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();
                // Establecer método de comunicación. Por defecto GET.
                myConnection.setRequestMethod("GET");
                if (myConnection.getResponseCode() == 200)
                {
                    // Conexión exitosa
                    // Creamos Stream para la lectura de datos desde el servidor
                    InputStream responseBody = myConnection.getInputStream();
                    InputStreamReader responseBodyReader = new InputStreamReader(responseBody, StandardCharsets.UTF_8);
                    // Creamos Buffer de lectura
                    BufferedReader bR = new BufferedReader(responseBodyReader);
                    String line;
                    StringBuilder responseStrBuilder = new StringBuilder();
                    // Leemos el flujo de entrada
                    while ((line = bR.readLine()) != null)
                    {
                        responseStrBuilder.append(line);
                    }
                    // Parseamos respuesta en formato JSON
                    result = new JSONArray(responseStrBuilder.toString());
                    // Nos quedamos solamente con la primera
                    posicion = 0;
                    jsonobject = result.getJSONObject(posicion);
                    // Sacamos dato a dato obtenido
                    responseBody.close();
                    responseBodyReader.close();
                    myConnection.disconnect();
                }
                else
                {
                    // Error en la conexión
                    Log.println(Log.ERROR, "Error", "¡Conexión fallida!");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ERROR, "Error", "¡Conexión fallida!");
            }
            return (null);
        }

        protected void onPostExecute(String mensaje)
        {
            // Añado los apuntes obtenidos a la lista
            try
            {
                apuntes.clear();
                if (result != null)
                {
                    int longitud = result.length();
                    for (int i = 0; i < longitud; i++)
                    {
                        jsonobject = result.getJSONObject(i);
                        apuntes.add(jsonobject.getString("idApunte") + "  -  "
                                + jsonobject.getString("fechaApunte") + "  -  "
                                + jsonobject.getString("textoApunte"));
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
            adapter.notifyDataSetChanged();
        }
    }

    // Alta Apuntes
    private class AltaRemota extends AsyncTask<Void, Void, String>
    {
        // Atributos
        String fechaApunte, textoApunte, idCuadernoFK;
        // Constructor
        public AltaRemota(String fechaApunte, String textoApunte, String idCuadernoFK)
        {
            this.fechaApunte = fechaApunte;
            this.textoApunte = textoApunte;
            this.idCuadernoFK = idCuadernoFK;
        }
        // Inspectoras
        protected void onPreExecute() {}
        protected String doInBackground(Void... argumentos)
        {
            try
            {
                // Crear la URL de conexión al API
                URL url = new URL("http://"+ip+"/ApiRest/apuntes.php");
                // Crear la conexión HTTP
                HttpURLConnection myConnection = (HttpURLConnection) url.openConnection();
                // Establecer método de comunicación
                myConnection.setRequestMethod("POST");
                // Conexión exitosa
                HashMap<String, String> postDataParams = new HashMap<>();
                postDataParams.put("fechaApunte", this.fechaApunte);
                postDataParams.put("textoApunte", this.textoApunte);
                postDataParams.put("idCuadernoFK", this.idCuadernoFK);
                myConnection.setDoInput(true);
                myConnection.setDoOutput(true);
                OutputStream os = myConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(getPostDataString(postDataParams));
                writer.flush();
                writer.close();
                os.close();
                myConnection.getResponseCode();
                if (myConnection.getResponseCode() == 200)
                {
                    // Success
                    myConnection.disconnect();
                }
                else
                {
                    // Error handling code goes here
                    Log.println(Log.ASSERT, "Error", "Error");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ASSERT, "Excepción", e.getMessage());
            }
            return (null);
        }
        protected void onPostExecute(String mensaje) {}

        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet())
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    result.append("&");
                }
                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return result.toString();
        }
    }

    // Baja apuntes
    private class BajaRemota extends AsyncTask<Void, Void, String>
    {
        // Atributos
        String idApunte;
        // Constructor
        public BajaRemota(String id)
        {
            this.idApunte = id;
        }
        // Inspectores
        protected void onPreExecute() {}
        @Override
        protected String doInBackground(Void... voids)
        {
            try
            {
                // Crear la URL de conexión al API
                URI baseUri = new URI("http://"+ip+"/ApiRest/apuntes.php");
                String[] parametros = {"id", this.idApunte};
                URI uri = applyParameters(baseUri, parametros);
                // Create connection
                HttpURLConnection myConnection = (HttpURLConnection) uri.toURL().openConnection();
                // Establecer método. Por defecto GET
                myConnection.setRequestMethod("DELETE");
                if (myConnection.getResponseCode() == 200)
                {
                    // Success
                    Log.println(Log.ASSERT, "Resultado", "Apunte eliminado");
                    myConnection.disconnect();
                }
                else
                {
                    // Error handling code goes here
                    Log.println(Log.ASSERT, "Error", "Error");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ASSERT, "Excepción", e.getMessage());
            }
            return null;
        }
        protected void onPostExecute(String mensaje) {}
        URI applyParameters(URI uri, String[] urlParameters)
        {
            StringBuilder query = new StringBuilder();
            boolean first = true;
            for (int i = 0; i < urlParameters.length; i += 2)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    query.append("&");
                }
                try
                {
                    query.append(urlParameters[i]).append("=").append(URLEncoder.encode(urlParameters[i + 1], "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }
            try
            {
                return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query.toString(), null);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    // Modificar apuntes
    private class ModificacionRemota extends AsyncTask<Void, Void, String>
    {
        // Atributos
        String idApunte;
        String fechaApunte;
        String textoApunte;
        // Constructor
        public ModificacionRemota(String id, String fechaApunte, String textoApunte)
        {
            this.idApunte = id;
            this.fechaApunte = fechaApunte;
            this.textoApunte = textoApunte;
        }
        // Inspectores
        protected void onPreExecute() {}
        protected String doInBackground(Void... voids)
        {
            try
            {
                String response = "";
                Uri uri = new Uri.Builder().scheme("http").authority(ip).path("/ApiRest/apuntes.php")
                        .appendQueryParameter("idApunte", this.idApunte)
                        .appendQueryParameter("fechaApunte", this.fechaApunte)
                        .appendQueryParameter("textoApunte", this.textoApunte)
                        .build();
                // Create connection
                URL url = new URL(uri.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(15000);
                connection.setConnectTimeout(15000);
                connection.setRequestMethod("PUT");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                }
                else
                {
                    response = "";
                }
                connection.getResponseCode();
                if (connection.getResponseCode() == 200)
                {
                    // Success
                    Log.println(Log.ASSERT, "Resultado", "Apunte eliminado:" + response);
                    connection.disconnect();
                }
                else
                {
                    // Error handling code goes here
                    Log.println(Log.ASSERT, "Error", "Error");
                }
            }
            catch (Exception e)
            {
                Log.println(Log.ASSERT, "Excepción", e.getMessage());
            }
            return null;
        }
        protected void onPostExecute(String mensaje) {}
    }
}