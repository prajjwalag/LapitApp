package com.prajjwal.lapitapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName, mProfileStatus, mProfileFriendsCount;
    private Button mProfileSendReqBtn;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private FirebaseUser  mCurrentUser;

    private ProgressDialog mProgressDialog;

    private int mCurrentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        String user_id = getIntent().getStringExtra("user_id");

        mProfileImage = (ImageView) findViewById(R.id.profile_user_image);
        mProfileName = (TextView) findViewById(R.id.profile_user_name);
        mProfileStatus = (TextView) findViewById(R.id.profile_user_status);
        mProfileFriendsCount = (TextView) findViewById(R.id.profile_user_friends);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_add_friend_btn);

        mCurrentStatus = 0;

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please Wait, we are retrieving user's data");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_Request");
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String display_name = snapshot.child("name").getValue().toString();
                String user_status = snapshot.child("status").getValue().toString();
                String user_image = snapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(user_status);
                Picasso.get().load(user_image).placeholder(R.drawable.avatar).into(mProfileImage);

                //-------------------- Friend List/ Request Feature  ----------

                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if(snapshot.hasChild(user_id)) {
                            String req_type = snapshot.child(user_id).child("request_type").getValue().toString();
                            if(req_type.equals("received")) {
                                mCurrentStatus = 2;
                                mProfileSendReqBtn.setText("Accept Friend Request");
                            } else if(req_type.equals("sent")) {
                                mCurrentStatus = 1;
                                mProfileSendReqBtn.setText("Cancel Friend Request");
                            }
                        }

                        mProgressDialog.dismiss();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mProfileSendReqBtn.setEnabled(false);

                //--------------- Not friends State

                if(mCurrentStatus == 0) {
                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).child("request_type").
                    setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()) {
                                mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).child("request_type")
                                        .setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        mProfileSendReqBtn.setEnabled(true);
                                        mCurrentStatus = 1;
                                        mProfileSendReqBtn.setText("Cancel Friend Request");
                                        Toast.makeText(ProfileActivity.this, "Request Sent Successfully.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else {
                                Toast.makeText(ProfileActivity.this, "Failed Sending Request.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }

                //Cancel Request State

                if(mCurrentStatus == 1 ) {
                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id)
                            .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrentStatus = 0;
                                    mProfileSendReqBtn.setText("Send Friend Request");

                                }
                            });
                        }
                    });
                }
            }
        });



    }
}