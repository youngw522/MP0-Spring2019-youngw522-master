package edu.illinois.cs.cs125.spring2019.mp0;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Random;

import edu.illinois.cs.cs125.spring2019.mp0.lib.Locator;

/**
 * The main activity launched when your app runs.
 * <p>
 * While we've done our level best to keep this code <i>as simple</i> as possible, there is some inherent and
 * inescapable complexity to any Android app: even a simple one like this one.
 * <p>
 * <strong>So don't panic.</strong> There is a lot of code and a lot of ideas here that you don't understand yet and
 * won't for at least a few weeks. That's OK. You <i>should</i> squint through the code here and try to get a general
 * sense of what is going on. That's a big part of working with any unfamiliar codebase, even one as relatively small
 * as this one.
 * <p>
 * We're also going to thoroughly comment the code below so that you get a sense of what is going on. But you should
 * also refer to the MP writeup which also contains a lot of useful documentation and explanation.
 */
public final class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    /**
     * This component's tag for logging.
     * <p>
     * Android has a powerful and flexible logging system. Using a distinctive tag makes it easier to filter messages
     * from this part of our app.
     */
    private static final String TAG = "MP0:MainActivity";

    /**
     * Constant that identifies our request for fine-grained location permissions.
     * <p>
     * Android's new permission model requires that we prompt the user before being able to use dangerous permissions
     * like fine-grained location. How this works is that we trigger a prompt for the user to complete. When they
     * reply, Android notifies us through a callback. We use this arbitrary number to match the request and the
     * response. This number can be anything. (I like the number 8.)
     */
    private static final int REQUEST_FINE_LOCATION = 88;

    /**
     * Whether we can access fine-grained location.
     * <p>
     * This is an example of an <i>instance variable</i>. We'll learn more about them soon when we discuss objects
     * and object-oriented programming. But for now you can think of this variable as a kind of global variable
     * available to all of the functions defined below.
     * <p>
     * This particular variable records whether our app has been granted permission to access the user's fine-grained
     * location. We check this when the app starts up. If it is false, we ask the user for this permission. If they
     * refuse, we have to disable many features of the app that depend on location services working.
     */
    private boolean canAccessFineLocation = false;

    /** Whether location updates are enabled or not. */
    private boolean locationEnabled = true;

    /**
     * Our fused location provider client.
     * <p>
     * Android exposes location information to apps as a service, meaning that we are the client.
     * <p>
     * Fused location refers to combining information from GPS and from Google's database of the locations of Wifi
     * access points: that's one the things that the cars that take the pictures for Maps Street View are also doing.
     * By combining these two sources of location of information Android apps can accurately locate users both
     * outdoors, where GPS tends to work well, and indoors, where it does not.
     */
    private FusedLocationProviderClient fusedLocationProviderClient;

    /**
     * Parameters for our continuous location request.
     * <p>
     * There are different options available to use when we request location updates, such as how fast we want to
     * receive updates and how important accuracy is as opposed to, for example, battery consumption. This stores
     * data about the request that we make to the Android location service.
     */
    private LocationRequest locationRequest;

    /**
     * How often we request location updates, in ms.
     * <p>
     * See the description above about configuring our request to Google's location service. This constant specifies
     * how often we request updates: in this case, as quickly as every 5s. According to Android's documentation this
     * is the fastest that apps should request which need continuous location information.
     */
    private static final int LOCATION_REQUEST_RATE = 5000;

    /**
     * Callback run when new location updates are available.
     * <p>
     * So once we have asked Android to update us when the user moves, how does it notify us when that happens? On
     * Android this is done using what is called a <i>a callback function</i>. We tell Android that we want a
     * particular function in our app to run whenever the user's location changes. That function receives information
     * about the user's new position.
     * <p>
     * Our callback function itself is defined below. This stores a reference to it so that we can stop updates when
     * our app enters the background.
     */
    private LocationCallback locationCallback;

    /**
     * Size of our location array.
     * <p>
     * 720 values stores about an hour of data when receiving one update every five seconds.
     */
    private static final int LOCATION_ARRAY_SIZE = 720;

    /**
     * Array of longitude measurements.
     * <p>
     * There are better ways to store data like this, which we'll learn about shortly. But for MP0 we store pairs of
     * latitude and longitude returned by the location service in two separate arrays: one for longitude and the
     * second for latitude. Each pair of measurements is store at the same index is both arrays.
     * <p>
     * We also maintain a separate array of booleans indicating which location measurements are valid. Until we store
     * LOCATION_ARRAY_SIZE measurements, some values in the array contain invalid values. So we use the array of
     * booleans to determine which spots in the latitude and longitude arrays contain value positions.
     */
    private double[] longitudes = new double[LOCATION_ARRAY_SIZE];

    /** Array of latitude measurements. */
    private double[] latitudes = new double[LOCATION_ARRAY_SIZE];

    /** Array to save whether the location at an index is valid or not. */
    private boolean[] validLocations = new boolean[LOCATION_ARRAY_SIZE];

    /**
     * Index of our current location measurement in our array.
     * <p>
     * We increment this each time <i>before</i> we add a location measurement to our arrays. That way, in between
     * new measurements it still stores the position of the last measurement that we saved. Starting it at -1 means
     * that after we increment it for the first time it will be 0, which is the right spot in the index for the first
     * measurement.
     */
    private int currentLocationIndex = -1;

    /**
     * Whether we've received any location updates at all.
     * <p>
     * This flag is used to avoid trying to recenter the map if we have no valid location updates.
     */
    private boolean receivedLocation = false;

    /**
     * Whether we are generating random new locations or not.
     * <p>
     * To make the app a bit more interesting during development, when you aren't usually moving around, it also
     * includes the ability to wander randomly from a starting point. You help enable this feature by implementing
     * nextRandomLocation in the Locator library.
     * <p>
     * While the user is wandering we disable location updates. This flag is also used to decide whether to restart
     * wandering or use real location updates when restarting location tracking using the UI.
     */
    private boolean wandering = false;

    /**
     * Handler to repeatedly wander to new locations.
     * <p>
     * We implement wandering using an Android concept called a Handler. How this works is beyond the scope of this
     * MP, but it allows us to run a callback (declared below) at a regular interval, which updates the randomly
     * generated location using while wandering.
     */
    private Handler handler = null;

    /**
     * Task that performs the wandering update.
     * <p>
     * Wandering is implemented using a similar concept the callback idea used to receive location updates. And
     * again, we store a reference to the callback so that we can disable it when the app goes into the background.
     */
    private Runnable wanderRunnable;

    /**
     * Rate at which we wander to new locations.
     * <p>
     * Currently once per second to help with testing, so that you can rapidly view movement to new locations and
     * test whether your helper functions are working properly.
     */
    private static final int WANDERING_RATE = 1000;

    /**
     * Maximum distance (in decimal degrees) to wander at each step for latitude.
     * <p>
     * When random locations updates are set we move at most this much in either the latitude or longitude. Note that
     * decimal degrees translate to different physical distances depending where you are on the globe, so this isn't
     * really the right way to do this. But it's sufficient for our simple app.
     * <p>
     * Note that because longitude is between -180 and 180, we set the maximum wander for longitude to be twice that
     * of latitude. Around the University of Illinois that still doesn't work out to be equal in both directions, and
     * you might want to explore why...
     */
    private static final double MAX_LATITUDE_WANDERING_DISTANCE = 0.001;

    /** Maximum distance (in decimal degrees) to wander at each step for longitude. */
    private static final double MAX_LONGITUDE_WANDERING_DISTANCE = 0.002;

    /**
     * A random number generator to use when wandering.
     * <p>
     * Random number generators generate a stream of pseudo-random numbers. We call them pseudo-random because they
     * don't quite display as much randomness as actual random physical processes. But they are close enough for our
     * purposes.
     */
    private static Random random = new Random();

    /**
     * Reference to our the MapView contained in the user interface.
     * <p>
     * In Android a View is part of the interface presented to the user. In this app we use a MapView, which is
     * included as part of the Google Play Services library and powered by Google Maps. This makes it simple to
     * integrate a map view into an existing application. MapViews can be manipulated in a variety of ways, only a
     * few of which you'll find in this MP. We encourage you to explore this idea further!
     */
    private MapView mapView = null;

    /**
     * Reference to the GoogleMap instance used to control the MapView, set once it has initialized.
     * <p>
     * This is required to be able to reconfigure the map and add our custom markers.
     */
    private GoogleMap googleMap = null;

    /** Latitude of a very special location. */
    private static final double SIEBEL_CENTER_LATITUDE = 40.092802;
    /** Longitude of a very special location. */
    private static final double SIEBEL_CENTER_LONGITUDE = -88.220097;

    /** Whether or not the map is currently centered. */
    public boolean centered = false;

    /**
     * Run when the activity is created.
     * <p>
     * onCreate is the first part of what Android refers to as the app <i>lifecycle</i>. This function will be run
     * the first time that the app is started, and after any time it is destroyed. It does not run every time the app
     * comes into the foreground: for that, see onResume.
     * <p>
     * onCreate is the place where we do a lot of initial setup for our app: checking for permissions, adding
     * handlers to the UI components that need them (like buttons), establishing connections to various services
     * that the app uses, and any other initialization that needs to happen.
     * <p>
     * <strong>To receive full credit on the MP you need to make a tiny change to this function.</strong> You should
     * connect the "Center" button to the centerMap function implemented below. While this change is small, it will
     * take you a while. That's normal when you're getting started working with large software projects. It's also
     * common when you've used to working with large software projects!
     *
     * @param savedInstanceState state saved by a previous instance of this activity, if any.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * This loads the main layout for this Activity. On Android, each activity usually corresponds to a single
         * <i>screen</i> on the app that the user might see. Our app only has one screen, so also only has one activity.
         */
        setContentView(R.layout.activity_main);

        handler = new Handler();

        /*
         * Determine if we've been granted fine-grained location permissions. If not, trigger the permissions dialog
         * again.
         *
         * Note that it is considered good practice to only do this when the app is started and not repeatedly bother
         * the user with repeated requests. Apps are also expected to be able to continue to function in a limited
         * capacity if the user refuses to provide certain privileges.
         *
         * Also note that this process completes in onRequestPermissionResult below. This is one example of a
         * split-phase or asynchronous operation. We initiate the request here but expect Android to notify us of the
         * result by calling onRequestPermissionResult.
         */
        canAccessFineLocation =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!canAccessFineLocation) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        }
        // If location permissions are disabled disable the button that turns tracking on and off
        findViewById(R.id.enableLocation).setEnabled(canAccessFineLocation);

        /*
         * The next few sections of onCreate set up button handlers for various parts of the user interface. The way
         * that our app responds to input from the user is by registering a callback function run whenever the user
         * interacts with the UI component in some way.
         */
        ((ToggleButton) findViewById(R.id.enableLocation)).setOnCheckedChangeListener((v, setEnabled) -> {
            /*
             * So, for example, every time the user enables or disables location tracking using the "Start/Stop"
             * button, this code runs. We receive some information about what happened: in this case, the variable
             * setEnabled will be set to true if the toggle button is enabled (the user wants to enable location
             * tracking) and false if the toggle button is disabled (the user wants to disable location tracking). We
             * use that information to decide what to do.
             */
            enableOrDisableLocation(setEnabled);
        });

        ((Switch) findViewById(R.id.wander)).setOnCheckedChangeListener((v, shouldWander) -> {
            /*
             * Similarly to above, the following code runs whenever the user changes the wandering toggle switch,
             * with shouldWander set to true or false depending on the state of the switch.
             */
            wandering = shouldWander;
            enableOrDisableLocation(locationEnabled);
        });

        findViewById(R.id.center).setOnClickListener((v) -> {
            centerMap();
        });

        /*
         * The next section of code sets up our connection to Android's location service, allowing us to receive
         * regular updates as the user moves around. Here we request high-accuracy location updates at a fairly fast
         * rate: every 5s.
         */
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest()
            .setInterval(LOCATION_REQUEST_RATE)
            .setFastestInterval(LOCATION_REQUEST_RATE)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /*
         * In a similar technique to other parts of the app, Android notifies us about new location results using a
         * callback function. We create it here but actually add and remove it from the service in the onResume and
         * onPause functions below.
         */
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                /*
                 * So this code runs every time we receive a location update. Like we do here, it's usually a good
                 * idea to just pass the information you receive from a callback to your app by calling a function
                 * that you have defined. Here we pass the new location information to processNewLocation, which
                 * saves it in our array and updates the map as needed.
                 */
                if (locationResult == null) {
                    return;
                }
                Location lastLocation = locationResult.getLastLocation();
                processNewLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
            }
        };

        /*
         * Here we initialize the map view included as part of our user interface. The error handling is required due
         * due to the UI testing library that we are using.
         */
        try {
            mapView = findViewById(R.id.mapView);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        } catch (ClassCastException ignore) { }

        /*
         * Finally we also set up a callback function to be run repeatedly when location wandering is enabled. All
         * this does is call wanderToNewLocation to actual implement the wandering and then reschedule itself to run
         * 1000ms (1s) later.
         */
        wanderRunnable = () -> {
            wanderToNewLocation();
            handler.postDelayed(wanderRunnable, WANDERING_RATE);
        };
    }

    /*
     * onResume, onPause, and onDestroy are the remainder of the Android life cycle events. onResume is run when the
     * activity comes into the foreground, and onPause when the activity is no longer visible. onDestroy is called if
     * activity is actually removed from the system, which can happen either through a user action or because the app
     * hasn't run for a while and Android wants to save memory by removing it. Apps are expected to be able to handle
     * all phases of the activity lifecycle.
     */

    /**
     * When the app comes to the foreground we enable location tracking.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        enableOrDisableLocation(true);
    }

    /**
     * When the app goes to the background we disable location tracking.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        enableOrDisableLocation(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    /**
     * Enable or disable real location tracking and wandering.
     * <p>
     * This function is called by the click handler attached to the "Start/Stop" button and on lifecycle events like
     * onResume and onPause. It enables or disables real location tracking or fake location wandering, depending on
     * the settings of other UI buttons.
     *
     * @param enable whether to enable real location tracking.
     */
    private void enableOrDisableLocation(final boolean enable) {
        locationEnabled = enable;
        if (locationEnabled) {
            if (wandering) {
                handler.post(wanderRunnable);
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            } else {
                if (canAccessFineLocation) {
                    try {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    } catch (SecurityException unused) {
                        canAccessFineLocation = false;
                    }
                }
                handler.removeCallbacks(wanderRunnable);
            }
        } else {
            handler.removeCallbacks(wanderRunnable);
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Process a new location update.
     * <p>
     * Here we save the new latitude and longitude to our arrays and update the map with markers indicating different
     * characteristics of each location.
     *
     * @param latitude latitude of the new location
     * @param longitude longitude of the new location
     */
    public void processNewLocation(final double latitude, final double longitude) {
        /*
         * Update our index before saving a new value. Because we initialize it to -1 our first value goes in index 0
         * which is what we want. Note that modular arithmetic ensures that the array index always stays within the
         * bounds of the array.
         */
        currentLocationIndex = (currentLocationIndex + 1) % LOCATION_ARRAY_SIZE;

        /*
         * Save the new value at the same index in both arrays, and mark this location as valid.
         */
        latitudes[currentLocationIndex] = latitude;
        longitudes[currentLocationIndex] = longitude;
        validLocations[currentLocationIndex] = true;

        /*
         * Mark that we've received a location update
         */
        receivedLocation = true;

        /*
         * If we don't have a map there's nothing else to do.
         */
        if (googleMap == null) {
            return;
        }

        /*
         * If we do have a map, clear it and mark it as not centered, since it is likely that our new location will
         * result in the map moving.
         */
        googleMap.clear();
        centered = false;

        /*
         * Mark various locations on the map. We use one color (green) for the furthest position north, a second
         * color (blue) for any repeated locations, a third (red) for the latest location, and a fourth (orange) for
         * other locations.
         */
        int furthestNorth = Locator.farthestNorth(latitudes, longitudes, validLocations);
        for (int i = 0; i < LOCATION_ARRAY_SIZE; i++) {
            if (!(validLocations[i])) {
                continue;
            }
            float hue = BitmapDescriptorFactory.HUE_ORANGE;
            if (i == furthestNorth) {
                hue = BitmapDescriptorFactory.HUE_GREEN;
            } else if (i == currentLocationIndex) {
                hue = BitmapDescriptorFactory.HUE_RED;
            } else if (Locator.beenHere(i, latitudes, longitudes, validLocations)) {
                hue = BitmapDescriptorFactory.HUE_BLUE;
            }
            googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitudes[i], longitudes[i]))
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }
    }

    /**
     * Wander to a new fake location based on our current location.
     * <p>
     * This code is called periodically when wandering to fake location is enabled. It generates parameters for the
     * Locator.nextRandomLocation function that you will write, calls it, and then calls processNewLocation to fake
     * movement to the new random location.
     * <p>
     * Essentially this function implements a form of a <i>random walk</i>, which you can read more about online. But
     * you may want to inspect the results on the map once you have wandering enabled.
     */
    private void wanderToNewLocation() {
        /*
         * If we have a starting location recorded, use that. Otherwise, use the location of a very special place.
         */
        double currentLatitude, currentLongitude;
        if (receivedLocation) {
            currentLatitude = latitudes[currentLocationIndex];
            currentLongitude = longitudes[currentLocationIndex];
        } else {
            currentLatitude = SIEBEL_CENTER_LATITUDE;
            currentLongitude = SIEBEL_CENTER_LONGITUDE;
        }

        /*
         * Compute the transition parameters needed by nextRandomLocation.
         *
         * Note that the way that we calculate latitudeChange and longitudeChange is designed to ensure that we get a
         * mix of positive and negative values and don't drift off continuously in one direction or another. That's
         * too purposeful for actual wandering!
         */
        double transitionProbability = random.nextDouble();
        double latitudeChange =
            (random.nextDouble() * MAX_LATITUDE_WANDERING_DISTANCE * 2) - MAX_LATITUDE_WANDERING_DISTANCE;
        double longitudeChange =
            (random.nextDouble() * MAX_LONGITUDE_WANDERING_DISTANCE * 2) - MAX_LONGITUDE_WANDERING_DISTANCE;

        /*
         * Call Locator.nextRandomLocation and update our current location.
         */
        double[] newCoordinates = Locator.nextRandomLocation(currentLatitude, currentLongitude,
            transitionProbability, latitudeChange, longitudeChange);
        processNewLocation(newCoordinates[0], newCoordinates[1]);
    }

    /**
     * Recenter the map based on the last known location.
     * <p>
     * This function adjust the position of the map to be centered on the last known position. <strong>To earn full
     * credit on MP0, you should modify onCreate so that this function is called when the user clicks on the "Center"
     * button.</strong>
     */
    public void centerMap() {
        /*
         * If we don't have a map or haven't received a location we can't center.
         */
        if (!receivedLocation) {
            return;
        }

        /*
         * Mark that we centered the map.
         */
        centered = true;

        /*
         * If we don't have a map to center, return. This happens when testing the app using our UI testing
         * framework, which can't emulate the MapView interface component.
         */
        if (googleMap == null) {
            return;
        }

        /*
         * Otherwise move the map camera based on the last recorded position.
         */
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(
            new LatLng(latitudes[currentLocationIndex], longitudes[currentLocationIndex])
        ));
    }

    /*
     * Callback handler for our location permission request.
     *
     * As described above, we initiate our request for fine-grained location permissions in the onCreate method run
     * when the app starts. Once the user has responded, Android runs this callback to inform of the app of whether
     * the user granted the requested permission or not.
     */
    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
                                           final int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    canAccessFineLocation = true;
                    findViewById(R.id.enableLocation).setEnabled(true);
                }
            default:
                break;
        }
    }

    /**
     * Purely to save a reference to the map in the view so that we can use it later.
     *
     * @param setGoogleMap a reference to the GoogleMap controller for our MapView
     */
    @Override
    public void onMapReady(final GoogleMap setGoogleMap) {
        googleMap = setGoogleMap;
    }
}
