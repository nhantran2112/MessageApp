package com.project.meetingapp.listener;

import com.project.meetingapp.models.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);

    void chatWithUser(User user, String mess);

    void onMultipleUsersAction(Boolean isMultipleUsersSelected);
}
