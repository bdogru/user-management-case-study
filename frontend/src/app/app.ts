import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

interface User {
  id: number;
  name: string;
  email: string;
  version: number;
}

@Component({
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly http = inject(HttpClient);

  protected readonly users = signal<User[]>([]);
  protected readonly editing = signal<User | null>(null);
  protected readonly name = signal('');
  protected readonly email = signal('');
  protected readonly error = signal('');

  constructor() {
    this.load();
  }

  protected edit(user: User): void {
    this.editing.set(user);
    this.name.set(user.name);
    this.email.set(user.email);
  }

  protected cancel(): void {
    this.editing.set(null);
    this.name.set('');
    this.email.set('');
  }

  protected save(): void {
    const user = this.editing();
    // An update never sends the email: the backend rejects it.
    this.send(
      user
        ? this.http.put(`/api/users/${user.id}`, { name: this.name(), version: user.version })
        : this.http.post('/api/users', { name: this.name(), email: this.email() }),
    );
  }

  protected remove(user: User): void {
    this.send(this.http.delete(`/api/users/${user.id}`));
  }

  private send(request: Observable<unknown>): void {
    this.error.set('');
    request.subscribe({
      next: () => {
        this.cancel();
        this.load();
      },
      error: (e: HttpErrorResponse) => this.showError(e),
    });
  }

  private load(): void {
    this.http.get<User[]>('/api/users').subscribe({
      next: (users) => this.users.set(users),
      error: (e: HttpErrorResponse) => this.showError(e),
    });
  }

  private showError(e: HttpErrorResponse): void {
    // The backend's problem details carry a readable `detail`; without it (backend down) use the HTTP message.
    this.error.set(e.error?.detail ?? e.message);
  }
}
