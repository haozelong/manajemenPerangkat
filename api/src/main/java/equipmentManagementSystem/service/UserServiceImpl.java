package equipmentManagementSystem.service;

import com.mengyunzhi.core.exception.ObjectNotFoundException;
import com.mengyunzhi.core.service.YunzhiService;
import equipmentManagementSystem.entity.User;
import equipmentManagementSystem.input.PUser;
import equipmentManagementSystem.input.VUser;
import equipmentManagementSystem.respority.DepartmentRepository;
import equipmentManagementSystem.respority.Specs.UserSpecs;
import equipmentManagementSystem.respority.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.EntityNotFoundException;
import javax.xml.bind.ValidationException;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService{

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    public static final String DEFAULT_PASSWORD = "hebut";

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    private final YunzhiService<User> yunzhiService;

    public UserServiceImpl(UserRepository userRepository,
                           DepartmentRepository departmentRepository, YunzhiService<User> yunzhiService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.yunzhiService = yunzhiService;
    }

    @Override
    public Optional<User> getCurrentLoginUserWithoutTransaction() {
        logger.debug("????????????????????????????????????????????????????????????");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            UserDetailImpl userDetail;
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                userDetail = (UserDetailImpl) authentication.getPrincipal();
            } else if (authentication instanceof UserDetailImpl) {
                userDetail = (UserDetailImpl) authentication;
            } else if (authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            } else {
                throw new RuntimeException("?????????????????????");
            }
            return Optional.of(userDetail.getUser());
        }

        logger.debug("????????????????????????????????????");
        return Optional.empty();
    }

    @Override
    public void add(User user) {
        Assert.notNull(user.getName(), "name????????????");
        Assert.notNull(user.getDepartment(), "??????????????????");
        Assert.notNull(user.getSex(), "??????????????????");
        Assert.notNull(user.getJobNumber(), "??????????????????");
        Assert.notNull(user.getPhone(), "?????????????????????");
        Assert.notNull(user.getRole(), "??????????????????");
        user.setPassword(DEFAULT_PASSWORD);
        this.userRepository.save(user);
        if (user.getRole().equals(3L)) {
            user.getDepartment().setUser(user);
            this.departmentRepository.save(user.getDepartment());
        }
    }

    @Override
    public void delete(Long id) {
        this.userRepository.deleteById(id);
    }

    @Override
    public User getOneUnsavedUser() {
        return UserService.getOneUser();
    }

    @Override
    public User getCurrentLoginUser() {
        logger.debug("???????????????");
        User user = null;

        logger.debug("????????????????????????");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        logger.debug("??????????????????????????????");
        if (authentication != null && authentication.isAuthenticated()) {
            user = userRepository.findByUsername(authentication.getName());
        }

        return user;
    }

    @Override
    public Page<User> quaryAll(String name, String jobNumber, Pageable pageable) {
        if (this.getCurrentLoginUser().getRole() != 3){
            return (Page<User>) this.userRepository.query(null, name, jobNumber, pageable, this.getCurrentLoginUser().getId());
        }
        else {
            return this.userRepository.query(this.getCurrentLoginUser().getDepartment(),name, jobNumber,
                    pageable, this.getCurrentLoginUser().getId());
        }
    }

    @Override
    public List<User> getAllCharge() {
        return (List<User>) this.userRepository.findAll(UserSpecs.getAllCharge());
    }

    public User getOneSavedUser() {
        User user = this.getOneUnsavedUser();
        return this.userRepository.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return this.userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("?????????????????????"));
    }

    @Override
    public Page<User> getAll(Pageable pageable) {
        Long id = this.getCurrentLoginUser().getId();
        if (this.getCurrentLoginUser().getRole() != 3){
            return (Page<User>) this.userRepository.findAll(pageable);
        }
       else {
           return this.userRepository.getAllByDepartment(this.getCurrentLoginUser().getDepartment(),
                   pageable , this.getCurrentLoginUser().getId());
        }
    }


    @Override
    public Boolean isUsernameExist(String username) {
        logger.debug("??????????????????????????????");
        User user = this.userRepository.findByUsername(username);

        logger.debug("????????????????????????");
        if (user != null) {
            return true;
        } else {
            return user != null;
        }
    }


    @Override
    public User update(Long id, User user) {
        Assert.notNull(user.getName(), "name????????????");
        Assert.notNull(user.getDepartment(), "??????????????????");
        Assert.notNull(user.getSex(), "??????????????????");
        Assert.notNull(user.getJobNumber(), "??????????????????");
        Assert.notNull(user.getPhone(), "?????????????????????");
        Assert.notNull(user.getRole(), "??????????????????");
        User oldUser = this.getUserById(id);
        oldUser.setUsername(user.getUsername());
        oldUser.setName(user.getName());
        oldUser.setSex(user.getSex());
        oldUser.setDepartment(user.getDepartment());
        oldUser.setJobNumber(user.getJobNumber());
        oldUser.setPhone(user.getPhone());
        oldUser.setRole(user.getRole());
        return this.userRepository.save(oldUser);
    }


    @Override
    public User findByUsername(String username) {
        return this.userRepository.findByUsername(username);
    }

    @Override
    public boolean checkPasswordIsRight(VUser vUser) {
        logger.debug("??????????????????");
        User user = this.getCurrentLoginUser();

        logger.debug("????????????????????????");
        return user.verifyPassword(vUser.getPassword());
    }

    @Override
    public void updatePassword(VUser vUser) throws ValidationException {
        logger.debug("??????????????????");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("???????????????????????????");
        if (!this.checkPasswordIsRight(vUser)) {
            throw new ValidationException("??????????????????");
        }

        logger.debug("????????????");
        currentUser.setPassword(vUser.getNewPassword());
        this.userRepository.save(currentUser);
    }

    @Override
    public boolean verifyPassword() {
        logger.debug("??????????????????");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("?????????????????????????????????");
        return currentUser.verifyPassword(DEFAULT_PASSWORD);
    }

    @Override
    public void updatePhone(PUser pUser) throws ValidationException {
        logger.debug("??????????????????");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("????????????????????????");

        logger.debug("???????????????????????????");
        if (!currentUser.verifyPassword(pUser.getPassword())) {
            throw new ValidationException("??????????????????");
        }

        logger.debug("???????????????(?????????)");
        currentUser.setUsername(pUser.getNewPhoneNumber());
        this.userRepository.save(currentUser);
    }


    @Override
    public Boolean verifyPhoneNumber(String phoneNumber) {
        logger.debug("??????????????????");
        User currentUser = this.getCurrentLoginUser();

        logger.debug("?????????????????????????????????");
        return currentUser.getUsername().equals(phoneNumber);
    }

    @Override
    public Boolean existByPhone(String phoneNumber) {
        return this.userRepository.findByUsername(phoneNumber) != null;
    }


    /**
     * ????????????
     *
     * @param id ??????id
     * @author pj
     */
    @Override
    public void resetPassword(Long id) {
        User user = this.userRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("???????????????: " + id.toString()));
        user.setPassword(DEFAULT_PASSWORD);
        this.userRepository.save(user);
    }

    /**
     * ???????????????????????????Boolean ??????
     *
     * @param sex ???????????????
     * @return ??? false ??? true
     */
    public Boolean formatSex(String sex) throws ValidationException {
        String man = "???";

        String woman = "???";
        if (man.equals(sex)) {
            return false;
        }
        if (woman.equals(sex)) {
            return true;
        }

        throw new ValidationException("??????????????????????????????");
    }
}
