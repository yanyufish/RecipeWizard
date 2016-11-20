package com.recipewizard.recipewizard;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Recipe Wizard";

    // Tabbed main activity and swipe between fragments adapted from these two tutorials:
    // https://www.youtube.com/watch?v=zQekzaAgIlQ
    // https://developer.android.com/training/implementing-navigation/lateral.html#tabs

    // ViewPager that will display each section of the app when swiped
    ViewPager mViewPager;

    // PageAdapter that will provide fragments to each of the two main sections of the app
    PagerAdapter mPagerAdapter;

    // TabLayout that show the two tabs
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set up ViewPager, PagerAdapter, and TabLayout
        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mPagerAdapter = new FixedTabsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(mViewPager);
    }

    // PagerAdapter that displays the two tabs: Ingredients and Recipes
    public static class FixedTabsPagerAdapter extends FragmentPagerAdapter {

        public FixedTabsPagerAdapter(FragmentManager fm) {
           super(fm);
        }
        @Override
        public int getCount() {
            return 2;
        }
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new IngredientsListFragment();
                case 1:
                    return new RecipesListFragment();
                default:
                    return null;
            }
        }
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Ingredients";
                case 1:
                    return "Recipes";
                default:
                    return null;
            }
        }
    }

    // Master list of ingredients
    public static MasterIngredientsList mMasterIngredientsList;

    // Fragment view for Ingredients list
    public static class IngredientsListFragment extends Fragment {

        private static final int INGREDIENTS_LIST_REQUEST = 0;
        private static final String INGREDIENTS_LIST = "IngredientsList";
        private static final String CATEGORY_NAME = "CategoryName";
        private static final String CATEGORY_INDEX = "CategoryIndex";
        static final String FILE_NAME = "master_ingredient_list.ser";

        TextView mIngredientsTextView;
        GridView mIngredientsGridView;

        // Adapter for GridView
        IngredientsCategoryAdapter mAdapter;

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.ingredients_category_fragment, container, false);
            mIngredientsTextView = (TextView) rootView.findViewById(R.id.ingredientsTextView);
            mIngredientsGridView = (GridView) rootView.findViewById(R.id.ingredientsGridView);

            mIngredientsTextView.setText(R.string.ingredients_header);


            // Create a new IngredientsCategoryAdapter for the GridView
            mAdapter = new IngredientsCategoryAdapter(getActivity());

            // Create the list of ingredients
            mMasterIngredientsList = new MasterIngredientsList();

            // Add to adapter list
            for (String category : mMasterIngredientsList.getAllCategories()) {
                mAdapter.add(new IngredientsCategory(category, String.valueOf(mMasterIngredientsList.getCheckedCount(category))));
            }

            // Attach the adapter to the GridView
            mIngredientsGridView.setAdapter(mAdapter);

            // OnClickListener for categories
            mIngredientsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                    // Package list to build listview
                    String categoryName = ((TextView) v.findViewById(R.id.ingredientsCategoryName)).getText().toString();
                    ArrayList<Ingredient> listToPackage = mMasterIngredientsList.getCategoryList(categoryName);
                    int index = parent.indexOfChild(v);

                    // Go to Ingredients List Activity for that category
                    Intent explicitIntent = new Intent(getActivity(), IngredientListActivity.class);
                    explicitIntent.putExtra(CATEGORY_NAME, categoryName);
                    explicitIntent.putExtra(INGREDIENTS_LIST, listToPackage);
                    explicitIntent.putExtra(CATEGORY_INDEX, index);
                    startActivityForResult(explicitIntent, INGREDIENTS_LIST_REQUEST );
                }
            });

            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            // update master ingredient list and category count
            if (requestCode == INGREDIENTS_LIST_REQUEST) {
                if (resultCode == RESULT_OK) {
                    String category = data.getStringExtra(CATEGORY_NAME);
                    ArrayList<Ingredient> list = (ArrayList<Ingredient>) data.getSerializableExtra(INGREDIENTS_LIST);
                    mMasterIngredientsList.updateList(category, list);

                    // update GridView to show accurate checked items count
                    int index = data.getIntExtra(CATEGORY_INDEX, IngredientsCategoryAdapter.NOT_FOUND);
                    if (index != IngredientsCategoryAdapter.NOT_FOUND) {
                        View v = mIngredientsGridView.getChildAt(index);
                        if (v != null) {
                            TextView categoryCountTextView = (TextView) v.findViewById(R.id.ingredientsCategoryCount);
                            categoryCountTextView.setText(getActivity()
                                    .getString(R.string.checked_count,
                                            String.valueOf(mMasterIngredientsList.getCheckedCount(category))));
                        }
                    }

                }
            }


        }

        @Override
        public void onResume() {
            super.onResume();
            Log.i(TAG, "onResume");
            // Load saved Ingredients, if necessary
            if (mAdapter.getCount() == 0) {
                load();
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            Log.i(TAG, "onPause");
            // Save ingredients
            save();
        }

        // Save Ingredients list to file
        private void save() {
            try {
                FileOutputStream fileOutputStream = getActivity().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

                objectOutputStream.writeObject(mMasterIngredientsList.getMasterList());

                objectOutputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load stored Ingredients
        private void load() {
            try {
                FileInputStream fileInputStream  = new FileInputStream(FILE_NAME);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

                mMasterIngredientsList = new MasterIngredientsList((HashMap) objectInputStream.readObject());
                for (String category : mMasterIngredientsList.getAllCategories()) {
                    mAdapter.add(new IngredientsCategory(category, String.valueOf(mMasterIngredientsList.getCheckedCount(category))));
                }

                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    // Fragment view for Recipes list
    public static class RecipesListFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_holder, container, false);
            ((TextView) rootView.findViewById(android.R.id.text1)).setText("Recipes Place Holder");

            return rootView;
        }

        // Test to check that checked ingredients are getting seeing correctly
        // Get all checked ingredients by calling mMasterIngredientsList.getCheckedIngredients()
        // Returns ArrayList<Ingredients>
        @Override
        public void onResume() {
            super.onResume();
            Log.i(TAG, "RECIPE onResume");
            if (mMasterIngredientsList != null) {
                for (Ingredient ingredient : mMasterIngredientsList.getCheckedIngredients()) {
                    Log.i(TAG, "Recipe Fragment: " + ingredient.toString());
                }
            }
        }
    }

}
