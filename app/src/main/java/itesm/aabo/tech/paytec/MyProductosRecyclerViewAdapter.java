package itesm.aabo.tech.paytec;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MyProductosRecyclerViewAdapter extends RecyclerView.Adapter<MyProductosRecyclerViewAdapter.ViewHolder> {

    ArrayList<Producto> listaProductos;
    CustomClickListener listener;
    Context mContext;

    public MyProductosRecyclerViewAdapter(Context mContext, ArrayList<Producto> data, CustomClickListener listener) {
        this.listaProductos = data;
        this.listener=listener;
        this.mContext = mContext;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_productos, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mTitleView.setText(listaProductos.get(position).getNombre());
        holder.mSubtitleView.setText(listaProductos.get(position).getDescripcion());
        holder.mPriceView.setText("$".concat(String.valueOf(listaProductos.get(position).getPrecio())));
        holder.mFoto.setImageResource(listaProductos.get(position).getImagen());
        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClickListener(v, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mSubtitleView;
        public final TextView mPriceView;
        public final ImageView mFoto;
        public final Button mButton;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(R.id.textView2);
            mSubtitleView = (TextView) view.findViewById(R.id.textView3);
            mPriceView = (TextView) view.findViewById(R.id.textView5);
            mFoto = (ImageView) view.findViewById(R.id.imageView2);
            mButton = (Button) view.findViewById(R.id.button2);
        }
    }
}
