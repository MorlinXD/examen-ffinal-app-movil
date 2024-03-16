package com.example.examen_final;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.examen_final.ml.Bandera;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnFailureListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        OnFailureListener {

    public static int REQUEST_GALLERY = 222;
    public Bitmap mSelectedImage;
    public ImageView mImageView;
    public TextView txtResults;
    public static int REQUEST_CAMERA = 111;

    private GoogleMap mMap;

    public String Eti;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.image_view);
        txtResults = findViewById(R.id.txtresults);
        requestQueue = Volley.newRequestQueue(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());

                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void abrirGaleria(View view) {

        Intent i = new Intent(Intent.ACTION_PICK,

                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, REQUEST_GALLERY);
    }
    public void PersonalizedModel(View v) {
        try {
            String[] etiquetas = {"AR", "BE", "BR", "CO", "CR", "EC", "ES", "FR", "GB", "MX", "PT", "SE", "UY"};
            Bandera model = Bandera.newInstance(getApplicationContext());
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(convertirImagenATensorBuffer(mSelectedImage));
            Bandera.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            Eti = obtenerEtiquetayProbabilidad(etiquetas, outputFeature0.getFloatArray());
            InfoPais(Eti);
            model.close();
            txtResults.setText(Eti);

        } catch (Exception e) {
            txtResults.setText(e.getMessage());
        }
    }
    private String obtenerEtiquetayProbabilidad(String[] etiquetas, float[] probabilidades) {
        float valorMayor = Float.MIN_VALUE;
        int pos = -1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }

        return etiquetas[pos];
    }
    private ByteBuffer convertirImagenATensorBuffer(Bitmap mSelectedImage) {
        Bitmap imagen = Bitmap.createScaledBitmap(mSelectedImage, 224, 224, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        imagen.getPixels(intValues, 0, imagen.getWidth(), 0, 0, imagen.getWidth(), imagen.getHeight());
        int pixel = 0;
        for (int i = 0; i < imagen.getHeight(); i++) {
            for (int j = 0; j < imagen.getWidth(); j++) {
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        LatLng initialLocation = new LatLng(-1.831239, -78.183406);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 4));

    }

    public void InfoPais(String code) {
        String countryCode = code;
        String url = "http://www.geognos.com/api/en/countries/info/" + countryCode + ".json";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject countryInfo = response.getJSONObject("Results");
                            String countryName = countryInfo.getString("Name");
                            JSONObject capitalInfo = countryInfo.getJSONObject("Capital");
                            String capitalName = capitalInfo.getString("Name");
                            JSONObject countryCodes = countryInfo.getJSONObject("CountryCodes");
                            String iso2 = countryCodes.getString("iso2");
                            String iso3 = countryCodes.getString("iso3");
                            String fips = countryCodes.getString("fips");
                            String telPrefix = countryInfo.getString("TelPref");

                            // Guardar datos de GeoRectangle y GeoPt en variables
                            JSONObject geoRectangle = countryInfo.getJSONObject("GeoRectangle");
                            double west = geoRectangle.getDouble("West");
                            double east = geoRectangle.getDouble("East");
                            double north = geoRectangle.getDouble("North");
                            double south = geoRectangle.getDouble("South");

                            JSONArray geoPtArray = countryInfo.getJSONArray("GeoPt");
                            double geoPtLat = geoPtArray.getDouble(0);
                            double geoPtLng = geoPtArray.getDouble(1);

                            // Mostrar los datos en el TextView
                            String countryInfoText = "Country Name: " + countryName + "\n" +
                                    "Capital: " + capitalName + "\n" +
                                    "ISO 2 Code: " + iso2 + "\n" +
                                    "ISO 3 Code: " + iso3 + "\n" +
                                    "FIPS Code: " + fips + "\n" +
                                    "Tel Prefix: " + telPrefix;

                            txtResults.setText(countryInfoText);

                            // Llamar al método para pintar el cuadrado en el mapa
                            encerrarmapa(west, east, north, south);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            txtResults.setText("Error al obtener la información del país.");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.e("Error", "Error de red: " + error.getMessage());
                txtResults.setText("Error de red al obtener la información del país.");
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    public void encerrarmapa(double west, double east, double north, double south) {
        LatLng southWest = new LatLng(south, west);
        LatLng northEast = new LatLng(north, east);
        mMap.addPolygon(new PolygonOptions()
                .add(southWest, new LatLng(north, west), northEast, new LatLng(south, east))
                .strokeColor(Color.RED)
                .fillColor(Color.TRANSPARENT));


        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(southWest);
        builder.include(northEast);
        LatLngBounds bounds = builder.build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }

}