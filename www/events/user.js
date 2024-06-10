export class UserEvents {
  static join = "join";
  static leave = "leave";
  static clear = "clear";
  static update = "update";
}

class UserEventTarget extends EventTarget {
  /**
   * List of online users
   * @type {{username: String}[]}
   * @private
   */
  #users = [];
  /**
   * My user
   * @type {{username: String}}
   * @private
   */
  #me = {};

  /**
   * Number of active users
   * @type {number}
   * @private
   */
  #activeUsers = 0;

  get users() {
    return this.#users;
  }

  get me() {
    return this.#me;
  }

  set me(username) {
    if (!username.length) return;
    this.#me = { username };
    this.dispatchEvent(new CustomEvent(UserEvents.update));
  }

  get activeUsers() {
    return this.#activeUsers;
  }

  set activeUsers(count) {
    if (Number.isNaN(count)) return;
    this.#activeUsers = count;
  }

  join(username = "") {
    if (!username.length) return;
    if (this.#users.find((user) => user.username === username)) return;
    if (this.#me?.username === username) return;

    this.#users.push({ username });
    this.dispatchEvent(new CustomEvent(UserEvents.join, { detail: username }));
    this.dispatchEvent(new CustomEvent(UserEvents.update));
  }

  leave(username = "") {
    if (!username.length) return;
    this.#users = this.#users.filter((user) => user.username !== username);
    this.dispatchEvent(new CustomEvent(UserEvents.leave, { detail: username }));
    this.dispatchEvent(new CustomEvent(UserEvents.update));
  }

  clear() {
    this.#users = [];
    this.#me = {};
    this.#activeUsers = 0;
    this.dispatchEvent(new CustomEvent(UserEvents.clear));
    this.dispatchEvent(new CustomEvent(UserEvents.update));
  }
}

export const UserState = new UserEventTarget();
