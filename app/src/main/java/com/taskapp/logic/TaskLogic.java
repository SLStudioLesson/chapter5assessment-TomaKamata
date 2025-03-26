package com.taskapp.logic;

import java.time.LocalDate;
import java.util.List;

import com.taskapp.dataaccess.LogDataAccess;
import com.taskapp.dataaccess.TaskDataAccess;
import com.taskapp.dataaccess.UserDataAccess;
import com.taskapp.exception.AppException;
import com.taskapp.model.Log;
import com.taskapp.model.Task;
import com.taskapp.model.User;

public class TaskLogic {
    private final TaskDataAccess taskDataAccess;
    private final LogDataAccess logDataAccess;
    private final UserDataAccess userDataAccess;


    public TaskLogic() {
        taskDataAccess = new TaskDataAccess();
        logDataAccess = new LogDataAccess();
        userDataAccess = new UserDataAccess();
    }

    /**
     * 自動採点用に必要なコンストラクタのため、皆さんはこのコンストラクタを利用・削除はしないでください
     * @param taskDataAccess
     * @param logDataAccess
     * @param userDataAccess
     */
    public TaskLogic(TaskDataAccess taskDataAccess, LogDataAccess logDataAccess, UserDataAccess userDataAccess) {
        this.taskDataAccess = taskDataAccess;
        this.logDataAccess = logDataAccess;
        this.userDataAccess = userDataAccess;
    }

    /**
     * 全てのタスクを表示します。
     *
     * @see com.taskapp.dataaccess.TaskDataAccess#findAll()
     * @param loginUser ログインユーザー
     */
    public void showAll(User loginUser) {
        List<Task> tasks = taskDataAccess.findAll();
        if (tasks.size() <= 0) {
            return; // tasks.csvに有効行がない場合
        }
        for (Task task : tasks) {
            String representative = "";
            String status = "";
            if (task.getRepUser().getCode() == loginUser.getCode()) {
                representative = "あなたが担当しています";
            } else {
                representative = task.getRepUser().getName() + "が担当しています";
            }
            switch (task.getStatus()) {
                case 0:
                    status = "未着手";
                    break;
                case 1:
                    status = "着手中";
                    break;
                case 2:
                    status = "完了";
                    break;
                default:
                    break;
            }
            System.out.println(task.getCode() + ". タスク名：" + task.getName() +
                ", 担当者名：" + representative + ", ステータス：" + status);
        }
    }

    /**
     * 新しいタスクを保存します。
     *
     * @see com.taskapp.dataaccess.UserDataAccess#findByCode(int)
     * @see com.taskapp.dataaccess.TaskDataAccess#save(com.taskapp.model.Task)
     * @see com.taskapp.dataaccess.LogDataAccess#save(com.taskapp.model.Log)
     * @param code タスクコード
     * @param name タスク名
     * @param repUserCode 担当ユーザーコード
     * @param loginUser ログインユーザー
     * @throws AppException ユーザーコードが存在しない場合にスローされます
     */
    public void save(int code, String name, int repUserCode,
                    User loginUser) throws AppException {
        User repUser = userDataAccess.findByCode(repUserCode);
        if (repUser == null) {
            throw new AppException("存在するユーザーコードを入力してください");
        }
        // tasks.csvに1行登録
        Task task = new Task(code, name, 0, repUser);
        taskDataAccess.save(task);
        // log.csvに1行登録
        Log log = new Log(task.getCode(), loginUser.getCode(), task.getStatus(), LocalDate.now());
        logDataAccess.save(log);
        System.out.println(task.getName() + "の登録が完了しました。");
    }

    /**
     * タスクのステータスを変更します。
     *
     * @see com.taskapp.dataaccess.TaskDataAccess#findByCode(int)
     * @see com.taskapp.dataaccess.TaskDataAccess#update(com.taskapp.model.Task)
     * @see com.taskapp.dataaccess.LogDataAccess#save(com.taskapp.model.Log)
     * @param code タスクコード
     * @param status 新しいステータス
     * @param loginUser ログインユーザー
     * @throws AppException タスクコードが存在しない、またはステータスが前のステータスより1つ先でない場合にスローされます
     */
    public void changeStatus(int code, int status,
                            User loginUser) throws AppException {
        Task task = taskDataAccess.findByCode(code);
        if (task == null) {
            throw new AppException("存在するタスクコードを入力してください");
        }
        // 「未着手」(0)から「着手中」(1)、または「着手中」(1)から「完了」(2)以外の場合は登録不可
        if (!(task.getStatus() == 0 && status == 1) && !(task.getStatus() == 1 && status ==2)) {
            throw new AppException("ステータスは、前のステータスより1つ先のもののみを選択してください");
        }
        // 更新
        Task updateTask = new Task(code, task.getName(), status, task.getRepUser());
        taskDataAccess.update(updateTask);
        // log.csvに1行登録
        Log log = new Log(updateTask.getCode(), loginUser.getCode(), updateTask.getStatus(), LocalDate.now());
        logDataAccess.save(log);
        System.out.println("ステータスの変更が完了しました。");
    }

    /**
     * タスクを削除します。
     *
     * @see com.taskapp.dataaccess.TaskDataAccess#findByCode(int)
     * @see com.taskapp.dataaccess.TaskDataAccess#delete(int)
     * @see com.taskapp.dataaccess.LogDataAccess#deleteByTaskCode(int)
     * @param code タスクコード
     * @throws AppException タスクコードが存在しない、またはタスクのステータスが完了でない場合にスローされます
     */
    public void delete(int code) throws AppException {
        Task deleteTask = taskDataAccess.findByCode(code);
        if (deleteTask == null) {
            throw new AppException("存在するタスクコードを入力してください");
        }
        if (deleteTask.getStatus() != 2) {
            throw new AppException("ステータスが完了のタスクを選択してください");
        }
        taskDataAccess.delete(code);
        logDataAccess.deleteByTaskCode(code);
        System.out.println(deleteTask.getName() + "の削除が完了しました。");
    }
}