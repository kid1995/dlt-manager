import { bootstrapApplication } from '@angular/platform-browser'
import { appConfig } from './app/app.config'
import { AppComponent } from './app/app.component'

async function main(): Promise<void>{
  await bootstrapApplication(AppComponent, appConfig);
}

main().catch( err => {
  console.error('Error by bootstrapping Application', err)
})
   




