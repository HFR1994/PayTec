package itesm.aabo.tech.paytec;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {

    ArrayList<Producto> listaProductos;
    RecyclerView recyclerView;

    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.list);

        // 2. set layoutManger
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        listaProductos = new ArrayList<>();
        listaProductos.add(new Producto("Ensalada", "Juan Tortas", 40, R.drawable.ensalada));
        listaProductos.add(new Producto("Atun", "Mi Lunch", 50, R.drawable.ensalada));
        listaProductos.add(new Producto("Baguette", "Chopi Burger", 120, R.drawable.ensalada));

        MyProductosRecyclerViewAdapter adapter = new MyProductosRecyclerViewAdapter(getActivity(), listaProductos, new CustomClickListener() {
            @Override
            public void onClickListener(View v, int position) {
                GlobalVariables.setCart(String.valueOf(listaProductos.get(position).getPrecio()));
                ((MainActivity) Objects.requireNonNull(getActivity())).readtoPurchase();
            }
        });

        recyclerView.setAdapter(adapter);



        return rootView;
    }

}
