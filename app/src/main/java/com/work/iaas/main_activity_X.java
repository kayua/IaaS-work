package com.tensorlearning.wemeetapp;

import android.graphics.Outline;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.squareup.picasso.Picasso;
import com.tensorlearning.wemeetapp.Contacts.DataContacts;
import com.tensorlearning.wemeetapp.Notifications.FormatRequisition;
import com.tensorlearning.wemeetapp.Notifications.UserRequisitions;
import com.tensorlearning.wemeetapp.Publication.PublicationsFragment;
import com.tensorlearning.wemeetapp.Search.SearchFragment;
import com.tensorlearning.wemeetapp.Search.UserProfileSearch;
import com.tensorlearning.wemeetapp.Contacts.ContactsFragment;
import com.tensorlearning.wemeetapp.Solicitations.SolicitationsFragment;
import com.tensorlearning.wemeetapp.UserData.UserProfilePrivateData;

import java.util.ArrayList;


public class MainActivityX extends AppCompatActivity {


    ImageView imageViewSidebarUserIcon;
    ImageView imageViewSidebarWallpaper;
    ImageView imageViewSidebarGalleryFirst;
    ImageView imageViewSidebarGallerySecond;
    ImageView imageViewSidebarThird;
    ImageView imageViewSidebarGalleryLast;

    TextView textViewSidebarUserName;

    AppBarConfiguration multiFragmentBar;
    FloatingActionButton buttonMultipurpose;
    FloatingActionButton buttonMultipurposea;
    Toolbar layoutToolBar;
    View navigationBarView;
    SearchView searchViewInstanceBar;
    boolean searchBarState = false;
    DrawerLayout layoutDrawer;

    UserProfilePrivateData userProfilePrivateData = new UserProfilePrivateData();

    ArrayList<UserProfileSearch> listUsersFoundSearch = new ArrayList<>();
    ArrayList<FormatRequisition> listSolicitationFound;
    ArrayList<DataContacts>  contacts;

    FirebaseAuth authProcessing;
    String userUUID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.sidebar_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationBarView = navigationView.getHeaderView(0);
        authProcessing = FirebaseAuth.getInstance();
        userUUID = authProcessing.getUid();
        instanceComponents(navigationView, navigationBarView);
        ChangeColorBars();
        checkingSolicitationsAccept();
        searchViewInstanceBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                listUsersFoundSearch.clear();
                searchUsersWithName(newText);

                return false;
            }
        });

        searchViewInstanceBar.setOnCloseListener(() -> false);

        multiFragmentBar = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_gallery,
                R.id.nav_slideshow).setDrawerLayout(layoutDrawer).build();

        buttonMultipurpose.setOnClickListener(view -> showSolicitations());
        buttonMultipurposea.setOnClickListener(view -> showPublicationFragment());
        reshapeImagesSidebar();
        loadUserProfile();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, multiFragmentBar);
        NavigationUI.setupWithNavController(navigationView, navController);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.icon_home);

    }

    public void showSearchBar(){

        FragmentManager fragmentManagerSearch = getSupportFragmentManager();
        FragmentTransaction fragmentTransactionSearch = fragmentManagerSearch.beginTransaction();
        SearchFragment fragmentSearch = new SearchFragment();
        fragmentSearch.addListUserSearch(listUsersFoundSearch);
        fragmentSearch.setMyUserName(userProfilePrivateData.getUserFirstName());
        fragmentSearch.setUserImage(userProfilePrivateData.getUserImageProfile());
        fragmentSearch.setMyUserId(userProfilePrivateData.getUserUUID());
        fragmentTransactionSearch.add(R.id.nav_host_fragment, fragmentSearch);
        fragmentTransactionSearch.commit();
        searchBarState = true;
    }

    public void hideSearchBar(){

        FragmentManager fragmentManagerHome = getSupportFragmentManager();
        FragmentTransaction fragmentTransactionHome = fragmentManagerHome.beginTransaction();
        ContactsFragment fragmentHome = new ContactsFragment();
        fragmentTransactionHome.add(R.id.nav_host_fragment, fragmentHome);
        fragmentTransactionHome.commit();
        searchBarState = false;
    }

    @Override
    public void onBackPressed() {

        if(searchBarState){

            hideSearchBar();

        }else{

            super.onBackPressed();

        }

    }

    public void ChangeColorBars(){

        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
        View viewForm = getWindow().getDecorView();
        Window viewWindow = this.getWindow();
        viewWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        viewWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        viewWindow.setStatusBarColor(ContextCompat.getColor(this,R.color.white));
        int flagsOptions = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR| View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        viewForm.setSystemUiVisibility(flagsOptions);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    @Override
    public boolean onSupportNavigateUp() {

        NavController navigationController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navigationController, multiFragmentBar)
                || super.onSupportNavigateUp();

    }

    private void searchUsersWithName(String userName){
        
        FirebaseFirestore firebaseConnection = FirebaseFirestore.getInstance();

        firebaseConnection.collection("PublicUserDataShort")
                .whereEqualTo("userFirstName", userName).get()
                .addOnCompleteListener(tasksList -> {

                    if (tasksList.isSuccessful()) {

                        int numberMaxIterations = 0;

                        for (QueryDocumentSnapshot documentData : tasksList.getResult()) {

                            UserProfileSearch  newUserFound = new UserProfileSearch();
                            newUserFound.setUserFullName(documentData.getString("userFirstName"));
                            newUserFound.setUserDescription(documentData.getString("userEmployment"));
                            newUserFound.setUserHomeTown(documentData.getString("userHomeTown"));
                            newUserFound.setUserUUID(documentData.getString("userUUID"));
                            newUserFound.setUserNumberFollowers(Integer.toString(documentData.getLong("userNumberFollowers").intValue()));
                            newUserFound.setUserNumberFollowed(Integer.toString(documentData.getLong("userNumberFollowed").intValue()));
                            newUserFound.setUserIcon(documentData.getString("userImageProfile"));

                            listUsersFoundSearch.add(newUserFound);

                            if(numberMaxIterations>10){

                                break;

                            }

                            numberMaxIterations = numberMaxIterations+1;

                        }

                        showSearchBar();

                    }
                });

    }

    private void loadUserProfile(){

        FirebaseFirestore firebaseConnection = FirebaseFirestore.getInstance();
        DocumentReference dataBaseReference = firebaseConnection.collection("PrivateUserData").document(userUUID);
        dataBaseReference.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                DocumentSnapshot documentUser = task.getResult();

                if (documentUser.exists()) {

                    userProfilePrivateData = documentUser.toObject(UserProfilePrivateData.class);
                    loadProfileView();

                }
            }
        });
    }

    private void reshapeImagesSidebar(){

        ViewOutlineProvider provider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) { outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 16);  }
        };

        imageViewSidebarWallpaper.setOutlineProvider(provider);
        imageViewSidebarWallpaper.setClipToOutline(true);
        imageViewSidebarGalleryFirst.setOutlineProvider(provider);
        imageViewSidebarGalleryFirst.setClipToOutline(true);
        imageViewSidebarGallerySecond.setOutlineProvider(provider);
        imageViewSidebarGallerySecond.setClipToOutline(true);
        imageViewSidebarThird.setOutlineProvider(provider);
        imageViewSidebarThird.setClipToOutline(true);
        imageViewSidebarGalleryLast.setOutlineProvider(provider);
        imageViewSidebarGalleryLast.setClipToOutline(true);


    }

    private void loadProfileView(){

        Picasso.get().load(userProfilePrivateData.getUserImageProfile()).into(imageViewSidebarUserIcon);
        Picasso.get().load(userProfilePrivateData.getUserWallpaperProfile()).into(imageViewSidebarWallpaper);
        textViewSidebarUserName.setText(userProfilePrivateData.getUserFirstName());

    }

    private void instanceComponents(NavigationView navigationView, View view){

        layoutToolBar = findViewById(R.id.toolbar);

        setSupportActionBar(layoutToolBar);
        searchViewInstanceBar = layoutToolBar.findViewById(R.id.search_view);

        layoutDrawer = findViewById(R.id.drawer_layout);
        buttonMultipurpose = findViewById(R.id.fab);
        buttonMultipurposea = findViewById(R.id.faba);

        imageViewSidebarUserIcon = navigationView.getHeaderView(0).findViewById(R.id.imageViewSidebarUserIcon);
        imageViewSidebarWallpaper = navigationView.getHeaderView(0).findViewById(R.id.imageViewSidebarUserWallpaper);

        textViewSidebarUserName  = navigationView.getHeaderView(0).findViewById(R.id.textViewSidebarUserName);

        imageViewSidebarWallpaper = view.findViewById(R.id.imageViewSidebarUserWallpaper);
        imageViewSidebarGalleryFirst = view.findViewById(R.id.imageViewSidebarMostLiked);
        imageViewSidebarGallerySecond = view.findViewById(R.id.imageViewSidebarGallerySecond);
        imageViewSidebarThird = view.findViewById(R.id.imageViewSidebarGalleryThird);
        imageViewSidebarGalleryLast = view.findViewById(R.id.imageViewSidebarGalleyLast);

    }

    public void showSolicitations(){

        FirebaseFirestore firebaseConnection = FirebaseFirestore.getInstance();
        DocumentReference dataBaseReference = firebaseConnection.collection("PublicRequisition").document(userUUID);
        dataBaseReference.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                DocumentSnapshot document = task.getResult();

                if (document.exists()) {

                    UserRequisitions listRequisitions = document.toObject(UserRequisitions.class);
                    assert listRequisitions != null;
                    listSolicitationFound = new ArrayList<>(listRequisitions.friendshipRequest);

                    showSolicitationsFragment();
                }
            }
        });

    }

    public void showSolicitationsFragment(){

        FragmentManager fragmentManagerSearch = getSupportFragmentManager();
        FragmentTransaction fragmentTransactionSearch = fragmentManagerSearch.beginTransaction();
        SolicitationsFragment fragmentSearch = new SolicitationsFragment();
        fragmentSearch.addListNotification(listSolicitationFound);
        fragmentTransactionSearch.add(R.id.nav_host_fragment, fragmentSearch);
        fragmentTransactionSearch.commit();
        searchBarState = true;
    }

    public void showPublicationFragment(){

        FragmentManager fragmentManagerSearch = getSupportFragmentManager();
        FragmentTransaction fragmentTransactionSearch = fragmentManagerSearch.beginTransaction();
        PublicationsFragment fragmentSearch = new PublicationsFragment();

        fragmentTransactionSearch.add(R.id.nav_host_fragment, fragmentSearch);
        fragmentTransactionSearch.commit();
        searchBarState = true;
    }


    public void checkingSolicitationsAccept(){

        FirebaseFirestore firebaseConnection = FirebaseFirestore.getInstance();
        DocumentReference dataBaseReference = firebaseConnection.collection("PublicRequisition").document(userUUID);
        contacts = new ArrayList<>(userProfilePrivateData.getUserListContactsGeneric());

        dataBaseReference.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                DocumentSnapshot document = task.getResult();

                if (document.exists()) {

                    UserRequisitions listRequisitions = document.toObject(UserRequisitions.class);
                    assert listRequisitions != null;
                    listSolicitationFound = new ArrayList<>(listRequisitions.friendshipRequest);

                    for(FormatRequisition t: listSolicitationFound){
                        boolean newUser = true;
                        if(t.getTypeNotification().equals("AcceptInvite")){


                            for(DataContacts ts : contacts){

                                if(ts.getUserUUID().equals(t.getUserUUID())){
                                    newUser = false;

                                }

                            }

                            if(newUser){

                                Log.i("New Friend added", "================");
                                updateFriendStatePrivateProfile(t.getUserName(), "teste", t.getDateHour(), t.getUserUUID() );
                            }


                        }


                    }

                }
            }
        });


    }


    private void updateFriendStatePrivateProfile(String userFullName, String lastMessage, String hourLastMessage, String userUUID ) {

        FirebaseFirestore firebaseConnection = FirebaseFirestore.getInstance();
        String idUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference databaseUpdate = firebaseConnection.collection("/PrivateUserData/").document(idUser);
        DataContacts newContact = new DataContacts();
        newContact.setHourLastMessage(hourLastMessage);
        newContact.setLastMessage(lastMessage);
        newContact.setUserFullName(userFullName);
        newContact.setUserUUID(userUUID);
        databaseUpdate.update("userListContactsGeneric", FieldValue.arrayUnion(newContact))
                .addOnSuccessListener(aVoid -> Log.i("Feito","------------"));

    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("TESTANDO", "-------------------------------------------");

    }

}
