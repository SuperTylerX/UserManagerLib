package imp;

import config.AppConfig;
import model.Group;
import model.User;
import model.UserManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utils.Encrypt;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class UserManagerImp implements UserManager {

    private ArrayList<User> userList;
    private ArrayList<Group> groupList;
    private String CONFIG_FILE;

    public UserManagerImp(String Config_file) throws Exception  {
        this.userList = new ArrayList<>();
        this.groupList = new ArrayList<>();
        this.CONFIG_FILE = Config_file;
        try {
            this.loadUserListAndGroup();
        } catch (Exception e) {
            e.printStackTrace();
//            System.exit(-1);
            throw new IllegalArgumentException();
        }
    }

    public void loadUserListAndGroup() throws Exception {

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;

        try {
            jsonObject = (JSONObject) jsonParser.parse(new FileReader(CONFIG_FILE));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        JSONArray userlist = (JSONArray) jsonObject.get("userList");

            for (Object o : userlist) {
                Map user = (Map) o;
                String username = (String) user.get("userName");
                String password = (String) user.get("userPassword");
                String email = (String) user.get("userEmail");
                long userId = (long) user.get("userId");
                long userGroup = (long) user.get("userGroup");
                userList.add(new User(userId, username, email, password, userGroup));
            }



        JSONArray grouplist = (JSONArray) jsonObject.get("groupList");
        for (Object o : grouplist) {
            Map group = (Map) o;
            long groupId = (long) group.get("groupId");
            String groupName = (String) group.get("groupName");
            long groupParent = (long) group.get("groupParent");
            groupList.add(new Group(groupId, groupName, groupParent));
        }

        // check non-existing group
        for (Group g : this.groupList) {
            boolean valid = false;
            for (Group g2 : this.groupList) {
                if (g.getGroupParent() == g2.getGroupId() || g.getGroupParent() == 0) {
                    valid = true;
                    break;
                }
            }
            if (!valid) {
                throw new Exception("None Parent Group Detected!");
            }
        }

        // check undefined group
        for (User u: this.userList){
            boolean valid = false;
            for(Group g: this.groupList){
                if(u.getUserGroup() == g.getGroupId()){
                    valid = true;
                    break;
                }
            }
            if(!valid){
                throw new Exception("undefined group!");
            }
        }
    }

    public User authUser(String username, String password) {

        String encryptPassword = Encrypt.sha256EncryptSalt(password, AppConfig.getInstance().PASSWORD_SALT);
        for (User user : userList) {
            if (user.getUserName().equals(username) && user.getUserPassword().equals(encryptPassword)) {
                return user.clone();
            }
        }
        return null;
    }

    public String getUserNameById(long id) {
        for (User user : userList) {
            if (id == user.getUserId()) {
                return user.getUserName();
            }
        }
        return null;
    }

    public long getUserIdByName(String userName) {
        for (User user : userList) {
            if (userName.equals(user.getUserName())) {
                return user.getUserId();
            }
        }
        return -1;
    }

    public User getUserById(long id) {
        for (User user : userList) {
            if (id == user.getUserId()) {
                return user.clone();
            }
        }
        return null;
    }

    public long getUserGroupIdByUserID(long id) {
        for (User user : userList) {
            if (id == user.getUserId()) {
                return user.getUserGroup();
            }
        }
        return -1;
    }

    public Group getUserGroupByGroupId(long id) {
        for (Group group : groupList) {
            if (id == group.getGroupId()) {
                return group.clone();
            }
        }
        return null;
    }

    public String getGroupNameByGroupId(long groupId) {
        if (groupId == 0) {
            return "public";
        }
        for (Group group : groupList) {
            if (groupId == group.getGroupId()) {
                return group.getGroupName();
            }
        }
        return null;
    }

    // This method finds all the children and itself
    public ArrayList<Group> findChildren(long groupId) throws Exception {
        ArrayList<Group> _list = new ArrayList<>();
        if (groupId != 1) {
            _list.add(getUserGroupByGroupId(groupId));
        }
        findChildren(groupId, _list);
        return _list;
    }


    // This method only finds children
    public void findChildren(long groupId, ArrayList<Group> l) throws Exception {
        // if the group is admins
        if (groupId == 1) {
            l.addAll(this.groupList);
            return;
        }

        for (Group group : this.groupList) {
            if (group.getGroupParent() == groupId) {
                if (l.contains(group)) {
                    throw new Exception("Circular parent-child definition!!!");
                }
                l.add(group);
                findChildren(group.getGroupId(), l);
            }
        }
    }

    // check Group Validation
    public boolean checkGroupValidity(long userID, long postGroupID) throws Exception {
        long groupID = getUserGroupIdByUserID(userID);
        if (groupID == 1 || postGroupID == 0)
            return true;

        ArrayList<Group> validGroups = findChildren(groupID);
        for (Group g : validGroups) {
            if (g.getGroupId() == postGroupID)
                return true;
        }
        return false;

    }
}
