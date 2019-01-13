package broadcast;

import enums.Users;

public interface AccountChanged {
    void wasChanged (Users newUser);
}
