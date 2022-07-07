package com.example.diglevel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CustomBullseye extends androidx.appcompat.widget.AppCompatImageView {


    private Bitmap mMarker;
    Context mContext;
    Paint p;

    //Java constructor
    public CustomBullseye(Context context) {
        super(context);
        mContext = context;
        init();
    }

    //XML constructor
    public CustomBullseye(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {


        p = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    float x, y=0.0f;

    @Override
    protected void onDraw(Canvas c) {
        /// TODO Auto-generated method stub

        super.onDraw(c);
        p.setColor(Color.BLACK);
        c.drawCircle(x,y,50,p);

    }



    public void setCircleXY(float x, float y) {
        this.x = x;
        this.y = y;

    }

}