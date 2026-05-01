package rkr.simplekeyboard.inputmethod.latin;
import rkr.simplekeyboard.inputmethod.R;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Typeface;
import android.widget.ImageView;

public class SuggestionStripView extends LinearLayout {
    public interface Listener {
        void onPickSuggestion(String suggestion);
        void onGridClicked();
        void onPaletteClicked();
    }

    private Listener mListener;
    private final List<TextView> mSuggestionViews = new ArrayList<>();
    private final int MAX_SUGGESTIONS = 3;

    public SuggestionStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupView(context);
    }

    private void setupView(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundColor(Color.parseColor("#25282C")); // Dark background like the reference
        
        // Even larger box height for a "premium" feel (80dp)
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, height));

        // Added back vertical padding for breathing room
        int paddingVertical = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        setPadding(0, paddingVertical, 0, paddingVertical);

        // Left Icon (Grid) - Clickable
        ImageView leftIcon = new ImageView(context);
        leftIcon.setImageResource(R.drawable.ic_grid);
        int iconPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        leftIcon.setPadding(iconPadding, 0, iconPadding, 0);
        leftIcon.setAlpha(0.9f);
        leftIcon.setOnClickListener(v -> {
            if (mListener != null) mListener.onGridClicked();
        });
        addView(leftIcon);

        // Suggestions Container
        LinearLayout suggestionsContainer = new LinearLayout(context);
        suggestionsContainer.setOrientation(HORIZONTAL);
        suggestionsContainer.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f));
        suggestionsContainer.setGravity(Gravity.CENTER);
        addView(suggestionsContainer);

        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            // Divider
            if (i > 0) {
                View divider = new View(context);
                int divWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
                int divHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
                LayoutParams divParams = new LayoutParams(divWidth, divHeight);
                divParams.gravity = Gravity.CENTER_VERTICAL;
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor("#35FFFFFF")); // Subtle white divider
                suggestionsContainer.addView(divider);
            }

            TextView tv = new TextView(context);
            tv.setLayoutParams(new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f));
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Reduced size as requested
            tv.setAllCaps(false);
            tv.setMaxLines(1);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
            
            // Middle one slightly larger and bold
            if (i == 1) {
                tv.setTypeface(null, Typeface.BOLD);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20); 
            }

            final int index = i;
            tv.setOnClickListener(v -> {
                String text = ((TextView) v).getText().toString();
                if (mListener != null && !text.isEmpty()) {
                    mListener.onPickSuggestion(text);
                }
            });
            mSuggestionViews.add(tv);
            suggestionsContainer.addView(tv);
        }

        // Right Icon (Palette) - Clickable
        ImageView rightIcon = new ImageView(context);
        rightIcon.setImageResource(R.drawable.ic_palette);
        rightIcon.setPadding(iconPadding, 0, iconPadding, 0);
        rightIcon.setAlpha(0.9f);
        rightIcon.setOnClickListener(v -> {
            if (mListener != null) mListener.onPaletteClicked();
        });
        addView(rightIcon);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setSuggestions(List<String> suggestions) {
        clear();
        if (suggestions.isEmpty()) return;

        if (suggestions.size() == 1) {
            // Put in the middle
            TextView tv = mSuggestionViews.get(1);
            tv.setText(suggestions.get(0));
            tv.setVisibility(VISIBLE);
        } else {
            for (int i = 0; i < Math.min(MAX_SUGGESTIONS, suggestions.size()); i++) {
                TextView tv = mSuggestionViews.get(i);
                tv.setText(suggestions.get(i));
                tv.setVisibility(VISIBLE);
                if (i > 0) {
                    View divider = ((LinearLayout)tv.getParent()).getChildAt(i * 2 - 1);
                    divider.setVisibility(VISIBLE);
                }
            }
        }
    }

    public void clear() {
        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            TextView tv = mSuggestionViews.get(i);
            tv.setText("");
            tv.setVisibility(INVISIBLE);
            if (i > 0) {
                View divider = ((LinearLayout)tv.getParent()).getChildAt(i * 2 - 1);
                divider.setVisibility(INVISIBLE);
            }
        }
    }
}
